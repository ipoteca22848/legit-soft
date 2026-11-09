use crate::{
    download::{batch, progress, unzip, Job, Net},
    model::*,
};
use anyhow::{bail, Context, Result};
use serde_json::{json, Value};
use std::{
    collections::{HashMap, HashSet},
    path::{Path, PathBuf},
    process::{Command, Stdio},
};
use tauri::AppHandle;
pub const MANIFEST: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
fn os() -> &'static str {
    if cfg!(target_os = "windows") {
        "windows"
    } else if cfg!(target_os = "macos") {
        "osx"
    } else {
        "linux"
    }
}
pub fn allowed(rules: &[Rule]) -> bool {
    if rules.is_empty() {
        return true;
    }
    let mut allow = false;
    for r in rules {
        let matches_os = r
            .os
            .as_ref()
            .map(|o| {
                o.name.as_deref().map(|n| n == os()).unwrap_or(true)
                    && o.arch
                        .as_deref()
                        .map(|a| {
                            a == std::env::consts::ARCH
                                || (a == "amd64" && cfg!(target_arch = "x86_64"))
                        })
                        .unwrap_or(true)
                    && o.version
                        .as_ref()
                        .map(|v| {
                            regex::Regex::new(v)
                                .map(|re| {
                                    re.is_match(&sysinfo::System::os_version().unwrap_or_default())
                                })
                                .unwrap_or(false)
                        })
                        .unwrap_or(true)
            })
            .unwrap_or(true);
        // No demo, custom resolution, quick play or optional features are enabled.
        if matches_os && r.features.values().all(|expected| !*expected) {
            allow = r.action == "allow";
        }
    }
    allow
}
fn coordinate(name: &str) -> Result<String> {
    let (name, extension) = name.split_once('@').unwrap_or((name, "jar"));
    let p: Vec<_> = name.split(':').collect();
    if p.len() < 3 || p.len() > 4 {
        bail!("Некорректная Maven-координата")
    };
    Ok(format!(
        "{}/{}/{}/{}-{}{}.{}",
        p[0].replace('.', "/"),
        p[1],
        p[2],
        p[1],
        p[2],
        p.get(3).map(|s| format!("-{s}")).unwrap_or_default(),
        extension
    ))
}
fn lib_key(name: &str) -> String {
    let p: Vec<_> = name.split(':').collect();
    format!(
        "{}:{}:{}",
        p.first().unwrap_or(&""),
        p.get(1).unwrap_or(&""),
        p.get(3).unwrap_or(&"")
    )
}
fn merge(mut base: Version, child: Version) -> Version {
    base.id = child.id;
    if child.main_class.is_some() {
        base.main_class = child.main_class
    }
    if child.logging.is_some() {
        base.logging = child.logging;
    }
    if child.java_version.is_some() {
        base.java_version = child.java_version
    }
    if child.asset_index.is_some() {
        base.asset_index = child.asset_index
    }
    if child.assets.is_some() {
        base.assets = child.assets
    }
    if child.minecraft_arguments.is_some() {
        base.minecraft_arguments = child.minecraft_arguments
    }
    base.arguments.game.extend(child.arguments.game);
    base.arguments.jvm.extend(child.arguments.jvm);
    let keys: HashSet<_> = child.libraries.iter().map(|l| lib_key(&l.name)).collect();
    base.libraries.retain(|l| !keys.contains(&lib_key(&l.name)));
    base.libraries.extend(child.libraries);
    base
}
async fn vanilla(net: &Net, id: &str) -> Result<Version> {
    let manifest = net.json(MANIFEST).await?;
    let entry = manifest["versions"]
        .as_array()
        .context("Манифест Mojang")?
        .iter()
        .find(|v| v["id"] == id)
        .context("Версия не найдена")?;
    let url = entry["url"].as_str().context("URL версии")?;
    // Version JSON is downloaded and SHA-1 checked by the installer before use.
    Ok(serde_json::from_value(net.json(url).await?)?)
}
async fn java(net: &Net, app: &AppHandle, dir: &Path, major: u32) -> Result<PathBuf> {
    if ![8, 17, 21, 25].contains(&major) {
        bail!("Неподдерживаемая Java {major}")
    }
    let root = dir.join("runtime").join(major.to_string());
    fn find(dir: &Path) -> Option<PathBuf> {
        for e in std::fs::read_dir(dir).ok()?.flatten() {
            let p = e.path();
            if p.is_dir() {
                if let Some(p) = find(&p) {
                    return Some(p);
                }
            } else if p
                .file_name()
                .is_some_and(|n| n == if cfg!(windows) { "javaw.exe" } else { "java" })
            {
                return Some(p);
            }
        }
        None
    }
    if let Some(p) = find(&root) {
        return Ok(p);
    }
    progress(app, &format!("Установка Java {major}"), 0, 1);
    let platform = if cfg!(windows) {
        "windows"
    } else if cfg!(target_os = "macos") {
        "mac"
    } else {
        "linux"
    };
    let arch = if cfg!(target_arch = "aarch64") {
        "aarch64"
    } else {
        "x64"
    };
    if !cfg!(windows) {
        bail!("Автоустановка Java в этой сборке рассчитана на Windows x64")
    }
    let meta=net.json(&format!("https://api.adoptium.net/v3/assets/latest/{major}/hotspot?architecture={arch}&image_type=jre&os={platform}&vendor=eclipse")).await?;
    let pkg = &meta
        .as_array()
        .context("Ответ Adoptium")?
        .first()
        .context("Java недоступна")?["binary"]["package"];
    let url = pkg["link"].as_str().context("URL Java")?;
    let hash = pkg["checksum"].as_str().context("SHA-256 Java")?;
    let archive = dir.join("downloads").join(format!("java-{major}.zip"));
    net.file(url, &archive, Some(("sha256", hash))).await?;
    let staging = root.with_extension(format!("{}-staging", uuid::Uuid::new_v4()));
    let a = archive.clone();
    let s = staging.clone();
    tokio::task::spawn_blocking(move || unzip(&a, &s, &[])).await??;
    if find(&staging).is_none() {
        bail!("Архив Java не содержит javaw.exe")
    }
    if root.exists() {
        tokio::fs::remove_dir_all(&root).await?
    }
    tokio::fs::rename(&staging, &root).await?;
    progress(app, "Java установлена", 1, 1);
    find(&root).context("Java не найдена")
}
#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub struct Prepared {
    #[serde(default)]
    pub schema_version: u32,
    pub version: Version,
    pub java: PathBuf,
    pub client: PathBuf,
    pub classpath: Vec<PathBuf>,
    pub natives: PathBuf,
    pub game_dir: PathBuf,
}
async fn artifacts(
    net: &Net,
    app: &AppHandle,
    dir: &Path,
    version: &Version,
    base_id: &str,
    parallel: usize,
) -> Result<(PathBuf, Vec<PathBuf>, PathBuf)> {
    let base = vanilla(net, base_id).await?;
    let client = dir
        .join("versions")
        .join(base_id)
        .join(format!("{base_id}.jar"));
    let art = base.downloads.get("client").context("Нет client.jar")?;
    progress(app, "Клиент Minecraft", 0, 1);
    net.file(&art.url, &client, art.sha1.as_deref().map(|h| ("sha1", h)))
        .await?;
    let libraries = dir.join("libraries");
    let natives = dir.join("natives");
    if natives.exists() {
        tokio::fs::remove_dir_all(&natives).await?
    }
    tokio::fs::create_dir_all(&natives).await?;
    let mut cp = Vec::new();
    let mut jobs = Vec::new();
    let mut extraction = Vec::new();
    let mut seen = HashSet::new();
    for lib in &version.libraries {
        if !allowed(&lib.rules) {
            continue;
        }
        let art = lib.downloads.artifact.clone().unwrap_or(Artifact {
            url: format!(
                "{}{}",
                lib.url
                    .as_deref()
                    .unwrap_or("https://libraries.minecraft.net/"),
                coordinate(&lib.name)?
            ),
            path: Some(coordinate(&lib.name)?),
            ..Default::default()
        });
        let relative = art.path.clone().unwrap_or(coordinate(&lib.name)?);
        let path = safe_join(&libraries, &relative)?;
        cp.push(path.clone());
        // Forge processors generate local Maven artifacts with empty URLs and no hash.
        if (!path.is_file() || art.sha1.is_some()) && seen.insert(path.clone()) {
            if art.url.is_empty() {
                if !path.is_file() {
                    bail!("Отсутствует локальная библиотека Forge: {}", path.display())
                }
            } else {
                jobs.push(Job {
                    url: art.url,
                    path,
                    sha1: art.sha1,
                });
            }
        }
        if let Some(classifier) = lib.natives.get(os()) {
            let classifier = classifier.replace(
                "${arch}",
                if cfg!(target_pointer_width = "64") {
                    "64"
                } else {
                    "32"
                },
            );
            if let Some(art) = lib.downloads.classifiers.get(&classifier) {
                let rel = art.path.as_ref().context("Путь natives")?;
                let path = safe_join(&libraries, rel)?;
                if seen.insert(path.clone()) {
                    jobs.push(Job {
                        url: art.url.clone(),
                        path: path.clone(),
                        sha1: art.sha1.clone(),
                    });
                }
                extraction.push((
                    path,
                    lib.extract
                        .as_ref()
                        .map(|e| e.exclude.clone())
                        .unwrap_or_else(|| vec!["META-INF/".into()]),
                ));
            } else {
                bail!("Не найдены natives {classifier}")
            }
        }
    }
    batch(net, app, "Библиотеки", jobs, parallel).await?;
    for (archive, exclude) in extraction {
        let n = natives.clone();
        tokio::task::spawn_blocking(move || unzip(&archive, &n, &exclude)).await??;
    }
    cp.push(client.clone());
    if let Some(index) = &version.asset_index {
        let id = index.id.as_deref().context("Индекс assets")?;
        let path = safe_join(&dir.join("assets/indexes"), &format!("{id}.json"))?;
        net.file(
            &index.url,
            &path,
            index.sha1.as_deref().map(|h| ("sha1", h)),
        )
        .await?;
        let asset: Value = serde_json::from_slice(&tokio::fs::read(path).await?)?;
        let objects = asset["objects"].as_object().context("Объекты assets")?;
        if asset["virtual"] == true || asset["map_to_resources"] == true {
            bail!("Legacy assets не поддерживаются; выберите Minecraft 1.16.5+")
        }
        let mut hashes = HashSet::new();
        let mut jobs = Vec::new();
        for obj in objects.values() {
            let hash = obj["hash"].as_str().context("Хеш assets")?;
            if hash.len() != 40 || !hash.bytes().all(|b| b.is_ascii_hexdigit()) {
                bail!("Некорректный хеш assets")
            };
            if !hashes.insert(hash.to_string()) {
                continue;
            }
            jobs.push(Job {
                url: format!(
                    "https://resources.download.minecraft.net/{}/{}",
                    &hash[..2],
                    hash
                ),
                path: dir.join("assets/objects").join(&hash[..2]).join(hash),
                sha1: Some(hash.into()),
            });
        }
        batch(net, app, "Ресурсы игры", jobs, parallel).await?;
    }
    if let Some(logging) = version.logging.as_ref().and_then(|l| l.client.as_ref()) {
        let path = safe_join(
            &dir.join("assets/log_configs"),
            logging
                .file
                .id
                .as_deref()
                .context("Имя конфигурации логирования")?,
        )?;
        net.file(
            &logging.file.url,
            &path,
            logging.file.sha1.as_deref().map(|h| ("sha1", h)),
        )
        .await?;
    }
    Ok((client, cp, natives))
}
pub async fn prepare(
    net: &Net,
    app: &AppHandle,
    dir: &Path,
    p: &Profile,
    parallel: usize,
) -> Result<Prepared> {
    tokio::fs::create_dir_all(dir).await?;
    if let Ok(bytes) = tokio::fs::read(dir.join("prepared.json")).await {
        if let Ok(prepared) = serde_json::from_slice::<Prepared>(&bytes) {
            if prepared.schema_version == 2
                && prepared.java.is_file()
                && prepared.client.is_file()
                && prepared.natives.is_dir()
                && prepared.classpath.iter().all(|p| p.is_file())
            {
                progress(app, "Установленная сборка готова", 1, 1);
                return Ok(prepared);
            }
        }
    }
    progress(app, "Манифест Mojang", 0, 1);
    let manifest = net.json(MANIFEST).await?;
    let entry = manifest["versions"]
        .as_array()
        .context("Манифест")?
        .iter()
        .find(|v| v["id"] == p.version)
        .context("Нет версии")?;
    let json_path = dir
        .join("versions")
        .join(&p.version)
        .join(format!("{}.json", p.version));
    net.file(
        entry["url"].as_str().context("URL версии")?,
        &json_path,
        entry["sha1"].as_str().map(|h| ("sha1", h)),
    )
    .await?;
    let base: Version = serde_json::from_slice(&tokio::fs::read(json_path).await?)?;
    let requested = base
        .java_version
        .as_ref()
        .map(|j| j.major_version)
        .unwrap_or(8);
    let major = if requested == 16 { 17 } else { requested };
    let java = java(net, app, dir, major).await?;
    let resolved_path = dir.join("resolved-version.json");
    let mut version = if resolved_path.exists() {
        serde_json::from_slice(&tokio::fs::read(&resolved_path).await?)?
    } else {
        let version = match p.loader.as_str() {
            "vanilla" => base.clone(),
            "fabric" | "quilt" => {
                let host = if p.loader == "fabric" {
                    "https://meta.fabricmc.net/v2"
                } else {
                    "https://meta.quiltmc.org/v3"
                };
                let loaders = net
                    .json(&format!("{host}/versions/loader/{}", p.version))
                    .await?;
                let list = loaders.as_array().context("Список загрузчиков")?;
                let item = list
                    .iter()
                    .find(|v| v["loader"]["stable"] == true)
                    .or_else(|| list.first())
                    .context("Нет совместимого загрузчика")?;
                let loader = item["loader"]["version"]
                    .as_str()
                    .context("Версия загрузчика")?;
                let child: Version = serde_json::from_value(
                    net.json(&format!(
                        "{host}/versions/loader/{}/{loader}/profile/json",
                        p.version
                    ))
                    .await?,
                )?;
                merge(base.clone(), child)
            }
            "forge" => {
                // Official installer performs Forge processors and writes its inherited manifest.
                artifacts(net, app, dir, &base, &p.version, parallel).await?;
                progress(app, "Установка Forge", 0, 1);
                let promos=net.json("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json").await?;
                let forge = promos["promos"][format!("{}-recommended", p.version)]
                    .as_str()
                    .or_else(|| promos["promos"][format!("{}-latest", p.version)].as_str())
                    .context("Forge недоступен для этой версии")?;
                let coord = format!("{}-{forge}", p.version);
                let installer = dir.join("downloads/forge-installer.jar");
                let installer_url=format!("https://maven.minecraftforge.net/net/minecraftforge/forge/{coord}/forge-{coord}-installer.jar");
                let checksum = net
                    .client
                    .get(format!("{installer_url}.sha1"))
                    .send()
                    .await?
                    .error_for_status()?
                    .text()
                    .await?;
                let checksum = checksum
                    .split_whitespace()
                    .next()
                    .context("SHA-1 установщика Forge")?;
                if checksum.len() != 40 || !checksum.bytes().all(|b| b.is_ascii_hexdigit()) {
                    bail!("Некорректная SHA-1 Forge")
                }
                net.file(&installer_url, &installer, Some(("sha1", checksum)))
                    .await?;
                let lp = dir.join("launcher_profiles.json");
                if !lp.exists() {
                    tokio::fs::write(lp, b"{\"profiles\":{}}").await?;
                }
                let log = std::fs::File::create(dir.join("forge-install.log"))?;
                let status = hidden(
                    Command::new(&java)
                        .arg("-jar")
                        .arg(&installer)
                        .arg("--installClient")
                        .arg(dir)
                        .current_dir(dir)
                        .stdout(log.try_clone()?)
                        .stderr(log),
                )
                .status()?;
                if !status.success() {
                    bail!("Установщик Forge завершился ошибкой; см. forge-install.log")
                }
                let mut child = None;
                for entry in std::fs::read_dir(dir.join("versions"))? {
                    let path = entry?.path();
                    if let Some(id) = path.file_name().and_then(|s| s.to_str()) {
                        if id.to_lowercase().contains("forge") {
                            let file = path.join(format!("{id}.json"));
                            if file.exists() {
                                child =
                                    Some(serde_json::from_slice::<Version>(&std::fs::read(file)?)?);
                            }
                        }
                    }
                }
                merge(base.clone(), child.context("Forge не создал манифест")?)
            }
            _ => bail!("Неизвестный загрузчик"),
        };
        tokio::fs::write(&resolved_path, serde_json::to_vec_pretty(&version)?).await?;
        version
    };
    if version.logging.is_none() {
        version.logging = base.logging.clone();
    }
    let (client, classpath, natives) =
        artifacts(net, app, dir, &version, &p.version, parallel).await?;
    if p.performance && p.loader != "vanilla" {
        let project = if p.loader == "forge" {
            "embeddium"
        } else {
            "sodium"
        };
        let mut visited = HashSet::new();
        crate::mods::install_modrinth(net, app, dir, p, project, None, &mut visited).await?;
    }
    progress(app, "Готово", 1, 1);
    let prepared = Prepared {
        schema_version: 2,
        version,
        java,
        client,
        classpath,
        natives,
        game_dir: dir.into(),
    };
    tokio::fs::write(
        dir.join("prepared.json"),
        serde_json::to_vec_pretty(&prepared)?,
    )
    .await?;
    Ok(prepared)
}
pub fn hidden(cmd: &mut Command) -> &mut Command {
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        cmd.creation_flags(0x08000000);
    }
    cmd
}
fn expand(s: &str, vars: &HashMap<&str, String>) -> Result<String> {
    let mut value = s.to_owned();
    for (k, v) in vars {
        value = value.replace(&format!("${{{k}}}"), v)
    }
    if value.contains("${") {
        bail!("Неподдерживаемый аргумент: {value}")
    }
    Ok(value)
}
fn arguments(values: &[Value], vars: &HashMap<&str, String>) -> Result<Vec<String>> {
    let mut out = Vec::new();
    for v in values {
        match v {
            Value::String(s) => out.push(expand(s, vars)?),
            Value::Object(o) => {
                let rules: Vec<Rule> =
                    serde_json::from_value(o.get("rules").cloned().unwrap_or(json!([])))?;
                if allowed(&rules) {
                    match &o["value"] {
                        Value::String(s) => out.push(expand(s, vars)?),
                        Value::Array(a) => {
                            for s in a {
                                out.push(expand(s.as_str().context("Аргумент")?, vars)?)
                            }
                        }
                        _ => bail!("Некорректный аргумент"),
                    }
                }
            }
            _ => bail!("Некорректные arguments"),
        }
    }
    Ok(out)
}
pub fn launch_command(
    prepared: &Prepared,
    p: &Profile,
    account: &Account,
    access_token: &str,
    total_memory_mb: u64,
) -> Result<Command> {
    let max = total_memory_mb.saturating_sub(2048).max(1024);
    if p.memory_mb < 1024 || p.memory_mb > max {
        bail!("Недостаточно ОЗУ: оставьте системе минимум 2 ГБ")
    }
    let cp = std::env::join_paths(&prepared.classpath)?
        .to_string_lossy()
        .into_owned();
    let dir = prepared.game_dir.to_string_lossy().into_owned();
    let assets = prepared
        .game_dir
        .join("assets")
        .to_string_lossy()
        .into_owned();
    let libs = prepared
        .game_dir
        .join("libraries")
        .to_string_lossy()
        .into_owned();
    let vars: HashMap<&str, String> = HashMap::from([
        ("auth_player_name", account.name.clone()),
        ("auth_uuid", account.id.replace('-', "")),
        ("auth_access_token", access_token.into()),
        (
            "auth_session",
            format!("token:{access_token}:{}", account.id),
        ),
        (
            "user_type",
            if account.kind == "offline" {
                "legacy".into()
            } else {
                "msa".into()
            },
        ),
        ("version_name", prepared.version.id.clone()),
        ("version_type", "release".into()),
        ("game_directory", dir.clone()),
        ("assets_root", assets.clone()),
        ("game_assets", assets),
        (
            "assets_index_name",
            prepared
                .version
                .asset_index
                .as_ref()
                .and_then(|a| a.id.clone())
                .unwrap_or_default(),
        ),
        (
            "natives_directory",
            prepared.natives.to_string_lossy().into_owned(),
        ),
        ("launcher_name", "Aurora".into()),
        ("launcher_version", env!("CARGO_PKG_VERSION").into()),
        ("classpath", cp.clone()),
        (
            "classpath_separator",
            if cfg!(windows) {
                ";".into()
            } else {
                ":".into()
            },
        ),
        ("library_directory", libs),
        ("user_properties", "{}".into()),
        ("clientid", account.client_id.clone()),
        ("auth_xuid", account.xuid.clone()),
    ]);
    let mut jvm = arguments(&prepared.version.arguments.jvm, &vars)?;
    if let Some(logging) = prepared
        .version
        .logging
        .as_ref()
        .and_then(|l| l.client.as_ref())
    {
        let path = safe_join(
            &prepared.game_dir.join("assets/log_configs"),
            logging.file.id.as_deref().context("Имя log config")?,
        )?;
        jvm.push(expand(
            &logging.argument,
            &HashMap::from([("path", path.to_string_lossy().into_owned())]),
        )?);
    }
    jvm.push("-Dlog4j2.formatMsgNoLookups=true".into());
    if !jvm.iter().any(|s| s == "-cp" || s == "-classpath") {
        jvm.extend(["-cp".into(), cp]);
    }
    if !jvm.iter().any(|s| s.starts_with("-Djava.library.path=")) {
        jvm.push(format!(
            "-Djava.library.path={}",
            prepared.natives.display()
        ));
    }
    if cfg!(target_os = "macos") && !jvm.contains(&"-XstartOnFirstThread".to_string()) {
        jvm.push("-XstartOnFirstThread".into());
    }
    // G1 baseline suitable for a client. Aikar's full server preset is intentionally not forced.
    jvm.retain(|s| !s.starts_with("-Xmx") && !s.starts_with("-Xms"));
    jvm.extend([
        format!("-Xms{}M", p.memory_mb.min(1024)),
        format!("-Xmx{}M", p.memory_mb),
        "-XX:+UseG1GC".into(),
        "-XX:+ParallelRefProcEnabled".into(),
        "-XX:MaxGCPauseMillis=100".into(),
        "-XX:+DisableExplicitGC".into(),
    ]);
    let game = if prepared.version.arguments.game.is_empty() {
        shlex::split(
            prepared
                .version
                .minecraft_arguments
                .as_deref()
                .context("Нет аргументов игры")?,
        )
        .context("Legacy arguments")?
        .into_iter()
        .map(|s| expand(&s, &vars))
        .collect::<Result<Vec<_>>>()?
    } else {
        arguments(&prepared.version.arguments.game, &vars)?
    };
    let log = std::fs::File::create(prepared.game_dir.join("launcher-game.log"))?;
    let mut cmd = Command::new(&prepared.java);
    hidden(&mut cmd)
        .args(jvm)
        .arg(
            prepared
                .version
                .main_class
                .as_deref()
                .context("Нет mainClass")?,
        )
        .args(game)
        .current_dir(&prepared.game_dir)
        .stdout(log.try_clone()?)
        .stderr(log)
        .stdin(Stdio::null());
    Ok(cmd)
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn feature_rules() {
        let rules: Vec<Rule> =
            serde_json::from_value(json!([{"action":"allow","features":{"is_demo_user":true}}]))
                .unwrap();
        assert!(!allowed(&rules));
    }
    #[test]
    fn maven() {
        assert_eq!(
            coordinate("org.lwjgl:lwjgl:3.3.3:natives-windows").unwrap(),
            "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows.jar"
        );
    }
    #[test]
    fn reject_unknown_placeholder() {
        assert!(expand("${missing}", &HashMap::new()).is_err());
    }
}
#[cfg(test)]
mod command_tests {
    use super::*;
    #[test]
    fn command_preserves_spaces_and_expands_identity_and_natives() {
        let dir = std::env::temp_dir().join(format!("aurora test {}", uuid::Uuid::new_v4()));
        std::fs::create_dir_all(&dir).unwrap();
        let version:Version=serde_json::from_value(json!({
   "id":"1.21.1","mainClass":"net.minecraft.client.main.Main",
   "assetIndex":{"id":"17","url":"https://example.com/index.json"},
            "logging":{"client":{"argument":"-Dlog4j.configurationFile=${path}","file":{"id":"client.xml","url":"https://example.com/client.xml"}}},
   "arguments":{"jvm":["-Djava.library.path=${natives_directory}","-cp","${classpath}"],
   "game":["--username","${auth_player_name}","--uuid","${auth_uuid}","--accessToken","${auth_access_token}","--gameDir","${game_directory}","--assetsDir","${assets_root}","--assetIndex","${assets_index_name}"]}
  })).unwrap();
        let p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Test".into(),
            version: "1.21.1".into(),
            loader: "vanilla".into(),
            memory_mb: 4096,
            performance: false,
            background: String::new(),
        };
        let prepared = Prepared {
            schema_version: 2,
            version,
            java: dir.join("javaw.exe"),
            client: dir.join("client.jar"),
            classpath: vec![dir.join("library one.jar"), dir.join("client.jar")],
            natives: dir.join("native files"),
            game_dir: dir.clone(),
        };
        let account = offline_account("Notch").unwrap();
        let cmd = launch_command(&prepared, &p, &account, "0", 16384).unwrap();
        let args: Vec<_> = cmd
            .get_args()
            .map(|a| a.to_string_lossy().into_owned())
            .collect();
        assert!(args.contains(&"-Xmx4096M".to_owned()));
        assert!(args.contains(&format!(
            "-Djava.library.path={}",
            prepared.natives.display()
        )));
        assert!(args.contains(&"net.minecraft.client.main.Main".to_owned()));
        assert!(args.contains(&account.id));
        assert!(args.contains(&format!(
            "-Dlog4j.configurationFile={}",
            dir.join("assets/log_configs/client.xml").display()
        )));
        let index = args.iter().position(|a| a == "--gameDir").unwrap();
        assert_eq!(args[index + 1], dir.to_string_lossy());
        let index = args.iter().position(|a| a == "-cp").unwrap();
        assert_eq!(
            std::env::split_paths(&args[index + 1]).collect::<Vec<_>>(),
            prepared.classpath
        );
        assert!(!args.iter().any(|a| a.contains("${")));
        drop(cmd);
        std::fs::remove_dir_all(dir).unwrap();
    }
}
