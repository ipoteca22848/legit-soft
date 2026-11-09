use crate::{
    download::{progress, Net},
    model::{safe_join, Profile},
};
use anyhow::{bail, Context, Result};
use serde_json::{json, Value};
use std::{collections::HashSet, path::Path};
use tauri::AppHandle;
fn compatible(v: &Value, p: &Profile, is_mod: bool) -> bool {
    v["game_versions"]
        .as_array()
        .is_some_and(|a| a.iter().any(|v| v == &p.version))
        && (!is_mod
            || v["loaders"]
                .as_array()
                .is_some_and(|a| a.iter().any(|v| v == &p.loader)))
}
pub fn install_modrinth<'a>(
    net: &'a Net,
    app: &'a AppHandle,
    dir: &'a Path,
    p: &'a Profile,
    project: &'a str,
    pinned: Option<&'a str>,
    visited: &'a mut HashSet<String>,
) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<()>> + Send + 'a>> {
    Box::pin(async move {
        if visited.len() > 150 {
            bail!("Слишком много зависимостей")
        }
        let project_data = net
            .json(&format!("https://api.modrinth.com/v2/project/{project}"))
            .await?;
        let id = project_data["id"].as_str().context("ID проекта")?;
        if !visited.insert(id.to_string()) {
            return Ok(());
        }
        let kind = project_data["project_type"]
            .as_str()
            .context("Тип проекта")?;
        let folder = match kind {
            "mod" => "mods",
            "resourcepack" => "resourcepacks",
            "shader" => "shaderpacks",
            _ => bail!("Modpack .mrpack требует отдельного импортера"),
        };
        if kind == "mod" && p.loader == "vanilla" {
            bail!("Для модов создайте профиль Fabric, Quilt или Forge")
        }
        let version = if let Some(v) = pinned {
            net.json(&format!("https://api.modrinth.com/v2/version/{v}"))
                .await?
        } else {
            let query = net
                .client
                .get(format!("https://api.modrinth.com/v2/project/{id}/version"))
                .query(&[("game_versions", json!([p.version]).to_string())]);
            let query = if kind == "mod" {
                query.query(&[("loaders", json!([p.loader]).to_string())])
            } else {
                query
            };
            let versions: Value = query.send().await?.error_for_status()?.json().await?;
            versions
                .as_array()
                .context("Версии Modrinth")?
                .first()
                .cloned()
                .context("Нет совместимой версии контента")?
        };
        if version["project_id"] != id || !compatible(&version, p, kind == "mod") {
            bail!("Несовместимая зависимость {project}")
        }
        if let Some(deps) = version["dependencies"].as_array() {
            for d in deps {
                if d["dependency_type"] == "required" {
                    if let Some(dep) = d["project_id"].as_str() {
                        install_modrinth(net, app, dir, p, dep, d["version_id"].as_str(), visited)
                            .await?;
                    } else if let Some(vid) = d["version_id"].as_str() {
                        let metadata = net
                            .json(&format!("https://api.modrinth.com/v2/version/{vid}"))
                            .await?;
                        let dep = metadata["project_id"]
                            .as_str()
                            .context("Проект зависимости")?;
                        install_modrinth(net, app, dir, p, dep, Some(vid), visited).await?;
                    } else {
                        bail!("Не удалось разрешить обязательную зависимость")
                    }
                }
            }
        }
        let files = version["files"].as_array().context("Файлы версии")?;
        let file = files
            .iter()
            .find(|f| f["primary"] == true)
            .or_else(|| files.first())
            .context("Нет файла")?;
        let filename = file["filename"].as_str().context("Имя файла")?;
        if filename.contains('/') || filename.contains('\\') {
            bail!("Недопустимое имя файла")
        }
        let path = safe_join(&dir.join(folder), filename)?;
        let sha = file["hashes"]["sha512"].as_str().context("Нет SHA-512")?;
        progress(
            app,
            &format!(
                "Контент: {}",
                project_data["title"].as_str().unwrap_or(project)
            ),
            0,
            1,
        );
        net.file(
            file["url"].as_str().context("URL файла")?,
            &path,
            Some(("sha512", sha)),
        )
        .await?;
        // Replace only the previous version of this same project, after the new file is verified.
        let lockdir = dir.join("content-locks");
        tokio::fs::create_dir_all(&lockdir).await?;
        let lock = safe_join(&lockdir, &format!("modrinth-{id}.json"))?;
        if let Ok(bytes) = tokio::fs::read(&lock).await {
            if let Ok(previous) = serde_json::from_slice::<Value>(&bytes) {
                if let Some(old) = previous["filename"].as_str() {
                    if old != filename {
                        let old = safe_join(&dir.join(folder), old)?;
                        if old.exists() {
                            tokio::fs::remove_file(old).await?
                        }
                    }
                }
            }
        }
        tokio::fs::write(
            lock,
            serde_json::to_vec(&json!({"filename":filename,"version":version["id"]}))?,
        )
        .await?;
        progress(app, "Контент установлен", 1, 1);
        Ok(())
    })
}
fn cf_key() -> Result<String> {
    keyring::Entry::new("AuroraLauncher", "curseforge")?
        .get_password()
        .context("Укажите CurseForge API key в настройках")
}
async fn cf(net: &Net, path: &str) -> Result<Value> {
    Ok(net
        .client
        .get(format!("https://api.curseforge.com/v1/{path}"))
        .header("x-api-key", cf_key()?)
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?)
}
pub async fn search(
    net: &Net,
    provider: &str,
    query: &str,
    kind: &str,
    p: &Profile,
) -> Result<Value> {
    if provider == "modrinth" {
        let project_type = match kind {
            "resourcepack" => "resourcepack",
            "shader" => "shader",
            _ => "mod",
        };
        let mut facets = vec![
            vec![format!("project_type:{project_type}")],
            vec![format!("versions:{}", p.version)],
        ];
        if project_type == "mod" && p.loader != "vanilla" {
            facets.push(vec![format!("categories:{}", p.loader)])
        }
        let v: Value = net
            .client
            .get("https://api.modrinth.com/v2/search")
            .query(&[
                ("query", query.to_string()),
                ("limit", "24".into()),
                ("facets", serde_json::to_string(&facets)?),
            ])
            .send()
            .await?
            .error_for_status()?
            .json()
            .await?;
        Ok(json!(v["hits"].as_array().context("Поиск Modrinth")?.iter().map(|h|json!({"id":h["project_id"],"title":h["title"],"description":h["description"],"icon":h["icon_url"],"downloads":h["downloads"]})).collect::<Vec<_>>()))
    } else if provider == "curseforge" {
        let class = match kind {
            "resourcepack" => 12,
            "shader" => 6552,
            _ => 6,
        };
        let loader = match p.loader.as_str() {
            "forge" => 1,
            "fabric" => 4,
            "quilt" => 5,
            _ => 0,
        };
        let mut request = net
            .client
            .get("https://api.curseforge.com/v1/mods/search")
            .header("x-api-key", cf_key()?)
            .query(&[
                ("gameId", "432".to_string()),
                ("classId", class.to_string()),
                ("searchFilter", query.into()),
                ("gameVersion", p.version.clone()),
                ("pageSize", "24".into()),
            ]);
        if class == 6 && loader != 0 {
            request = request.query(&[("modLoaderType", loader)])
        }
        let v: Value = request.send().await?.error_for_status()?.json().await?;
        Ok(json!(v["data"].as_array().context("Поиск CurseForge")?.iter().map(|h|json!({"id":h["id"].to_string(),"title":h["name"],"description":h["summary"],"icon":h["logo"]["thumbnailUrl"],"downloads":h["downloadCount"]})).collect::<Vec<_>>()))
    } else {
        bail!("Неизвестный каталог")
    }
}
pub fn install_curseforge<'a>(
    net: &'a Net,
    app: &'a AppHandle,
    dir: &'a Path,
    p: &'a Profile,
    id: u64,
    pinned: Option<u64>,
    visited: &'a mut HashSet<u64>,
) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<()>> + Send + 'a>> {
    Box::pin(async move {
        if visited.len() > 150 {
            bail!("Слишком много зависимостей")
        }
        if !visited.insert(id) {
            return Ok(());
        }
        let project = cf(net, &format!("mods/{id}")).await?;
        let class = project["data"]["classId"]
            .as_u64()
            .context("Тип контента")?;
        let folder = match class {
            6 => "mods",
            12 => "resourcepacks",
            6552 => "shaderpacks",
            _ => bail!("Неподдерживаемый тип CurseForge"),
        };
        let loader = match p.loader.as_str() {
            "forge" => "Forge",
            "fabric" => "Fabric",
            "quilt" => "Quilt",
            _ => "",
        };
        if class == 6 && loader.is_empty() {
            bail!("Нужен профиль с загрузчиком модов")
        }
        let file = if let Some(fid) = pinned {
            cf(net, &format!("mods/{id}/files/{fid}")).await?["data"].clone()
        } else {
            let list = cf(
                net,
                &format!("mods/{id}/files?gameVersion={}&pageSize=50", p.version),
            )
            .await?;
            list["data"]
                .as_array()
                .context("Файлы CurseForge")?
                .iter()
                .find(|f| {
                    f["gameVersions"].as_array().is_some_and(|a| {
                        a.iter().any(|v| v == &p.version)
                            && (class != 6 || a.iter().any(|v| v == loader))
                    })
                })
                .cloned()
                .context("Нет совместимого файла")?
        };
        let tags = file["gameVersions"]
            .as_array()
            .context("Совместимость файла")?;
        if !tags.iter().any(|v| v == &p.version)
            || (class == 6 && !tags.iter().any(|v| v == loader))
        {
            bail!("Несовместимый файл CurseForge")
        }
        if let Some(deps) = file["dependencies"].as_array() {
            for dep in deps {
                if dep["relationType"] == 3 {
                    install_curseforge(
                        net,
                        app,
                        dir,
                        p,
                        dep["modId"].as_u64().context("ID зависимости")?,
                        None,
                        visited,
                    )
                    .await?;
                }
            }
        }
        let filename = file["fileName"].as_str().context("Имя файла")?;
        if filename.contains('/') || filename.contains('\\') {
            bail!("Недопустимое имя файла")
        }
        let url = file["downloadUrl"]
            .as_str()
            .context("Автор запретил загрузку через API; установите файл с сайта CurseForge")?;
        let hashes = file["hashes"].as_array().context("Хеши CurseForge")?;
        let sha = hashes
            .iter()
            .find(|h| h["algo"] == 1)
            .and_then(|h| h["value"].as_str())
            .context("Нет SHA-1")?;
        progress(app, &format!("Установка {filename}"), 0, 1);
        net.file(
            url,
            &safe_join(&dir.join(folder), filename)?,
            Some(("sha1", sha)),
        )
        .await?;
        progress(app, "Контент установлен", 1, 1);
        Ok(())
    })
}
