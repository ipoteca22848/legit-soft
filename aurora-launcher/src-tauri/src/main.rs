#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]
mod auth;
mod download;
mod engine;
mod model;
mod mods;
use anyhow::{bail, Context, Result};
use model::*;
use serde_json::{json, Value};
use std::{
    collections::HashSet,
    path::PathBuf,
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc,
    },
};
use tauri::{Emitter, Manager};
use tokio::sync::{Mutex, Semaphore};
struct State {
    root: PathBuf,
    config: Mutex<Config>,
    cancel: Arc<AtomicBool>,
    operation: Arc<Semaphore>,
    running: AtomicBool,
}
fn err(e: anyhow::Error) -> String {
    format!("{e:#}")
}
async fn persist(s: &State, c: &Config) -> Result<()> {
    let path = s.root.join("config.json");
    let data = serde_json::to_vec_pretty(c)?;
    let tmp = s.root.join("config.new.json");
    tokio::fs::write(&tmp, &data).await?;
    if path.exists() {
        tokio::fs::copy(&path, s.root.join("config.bak.json")).await?;
        tokio::fs::remove_file(&path).await?;
    }
    tokio::fs::rename(tmp, path).await?;
    Ok(())
}
fn net(s: &State) -> Result<download::Net> {
    download::Net::new(s.cancel.clone())
}
fn memory() -> u64 {
    let mut sys = sysinfo::System::new();
    sys.refresh_memory();
    sys.total_memory() / 1024 / 1024
}
#[tauri::command]
async fn bootstrap(s: tauri::State<'_, State>) -> Result<Value, String> {
    Ok(
        json!({"config":s.config.lock().await.clone(),"memoryMb":memory(),"dataDirectory":s.root,"running":s.running.load(Ordering::Relaxed),"lastGameResult":std::fs::read_to_string(s.root.join("last-game-result.txt")).ok()}),
    )
}
#[tauri::command]
async fn versions(s: tauri::State<'_, State>) -> Result<Value, String> {
    async {
        let m = net(&s)?.json(engine::MANIFEST).await?;
        let all = m["versions"].as_array().context("Манифест Mojang")?;
        let supported = all
            .iter()
            .filter(|v| {
                v["type"] == "release"
                    && v["releaseTime"].as_str().is_some_and(|d| d >= "2021-01-14")
            })
            .map(|v| v["id"].clone())
            .collect::<Vec<_>>();
        Ok(json!(supported))
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn create_profile(
    s: tauri::State<'_, State>,
    name: String,
    version: String,
    loader: String,
    memory_mb: u64,
    performance: bool,
    background: String,
) -> Result<Profile, String> {
    async {
        let _guard = s
            .operation
            .clone()
            .try_acquire_owned()
            .context("Дождитесь окончания текущей операции")?;
        if name.trim().is_empty() || name.chars().count() > 60 {
            bail!("Название: 1–60 символов")
        }
        if version.is_empty()
            || version.len() > 30
            || !version.bytes().all(|b| b.is_ascii_digit() || b == b'.')
        {
            bail!("Выберите релиз Minecraft")
        }
        if !["vanilla", "fabric", "forge", "quilt"].contains(&loader.as_str()) {
            bail!("Неизвестный загрузчик")
        }
        if memory_mb < 1024 || memory_mb > memory().saturating_sub(2048).max(1024) {
            bail!("Недопустимое количество ОЗУ")
        }
        if !background.is_empty() && !background.starts_with("https://") {
            bail!("Видео: только HTTPS URL .mp4/.webm")
        }
        let p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: name.trim().into(),
            version,
            loader,
            memory_mb,
            performance,
            background,
        };
        tokio::fs::create_dir_all(profile_path(&s.root, &p.id)?).await?;
        let mut c = s.config.lock().await;
        c.profiles.push(p.clone());
        persist(&s, &c).await?;
        Ok(p)
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn set_memory(s: tauri::State<'_, State>, id: String, memory_mb: u64) -> Result<(), String> {
    async {
        if memory_mb < 1024 || memory_mb > memory().saturating_sub(2048).max(1024) {
            bail!("Оставьте системе не менее 2 ГБ ОЗУ")
        }
        let mut c = s.config.lock().await;
        c.profiles
            .iter_mut()
            .find(|p| p.id == id)
            .context("Нет профиля")?
            .memory_mb = memory_mb;
        persist(&s, &c).await
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn add_offline(s: tauri::State<'_, State>, name: String) -> Result<Account, String> {
    async {
        let a = offline_account(&name)?;
        let mut c = s.config.lock().await;
        if !c.accounts.iter().any(|v| v.id == a.id) {
            c.accounts.push(a.clone());
        }
        c.selected_account = Some(a.id.clone());
        persist(&s, &c).await?;
        Ok(a)
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn select_account(s: tauri::State<'_, State>, id: String) -> Result<(), String> {
    async {
        let mut c = s.config.lock().await;
        if !c.accounts.iter().any(|a| a.id == id) {
            bail!("Нет аккаунта")
        };
        c.selected_account = Some(id);
        persist(&s, &c).await
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn remove_account(s: tauri::State<'_, State>, id: String) -> Result<(), String> {
    async {
        let mut c = s.config.lock().await;
        c.accounts.retain(|a| a.id != id);
        if c.selected_account.as_deref() == Some(&id) {
            c.selected_account = c.accounts.first().map(|a| a.id.clone())
        };
        if let Ok(e) = auth::entry(&id) {
            let _ = e.delete_credential();
        }
        persist(&s, &c).await
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn save_settings(
    s: tauri::State<'_, State>,
    settings: Settings,
    curseforge_key: Option<String>,
) -> Result<(), String> {
    async {
        if !(1..=16).contains(&settings.parallel_downloads) {
            bail!("Потоки: 1–16")
        };
        if let Some(key) = curseforge_key {
            if !key.is_empty() {
                keyring::Entry::new("AuroraLauncher", "curseforge")?.set_password(&key)?;
            }
        }
        let mut c = s.config.lock().await;
        c.settings = settings;
        persist(&s, &c).await
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn microsoft_begin(s: tauri::State<'_, State>) -> Result<Value, String> {
    let c = s.config.lock().await.clone();
    auth::begin(&net(&s).map_err(err)?, &c.settings.microsoft_client_id)
        .await
        .map_err(err)
}
#[tauri::command]
async fn microsoft_poll(s: tauri::State<'_, State>, device_code: String) -> Result<Value, String> {
    async {
        let c = s.config.lock().await.clone();
        let result = auth::poll(&net(&s)?, &c.settings.microsoft_client_id, &device_code).await?;
        if result["pending"] == false {
            let a: Account = serde_json::from_value(result["account"].clone())?;
            let mut c = s.config.lock().await;
            c.accounts.retain(|old| old.id != a.id);
            c.accounts.push(a.clone());
            c.selected_account = Some(a.id);
            persist(&s, &c).await?;
        }
        Ok(result)
    }
    .await
    .map_err(err)
}
#[tauri::command]
fn cancel(s: tauri::State<'_, State>) {
    s.cancel.store(true, Ordering::Relaxed);
}
#[tauri::command]
async fn install(
    app: tauri::AppHandle,
    s: tauri::State<'_, State>,
    id: String,
) -> Result<(), String> {
    async {
        let _permit = s
            .operation
            .clone()
            .try_acquire_owned()
            .context("Уже выполняется операция")?;
        s.cancel.store(false, Ordering::Relaxed);
        let c = s.config.lock().await.clone();
        let p = c
            .profiles
            .iter()
            .find(|p| p.id == id)
            .context("Нет профиля")?;
        engine::prepare(
            &net(&s)?,
            &app,
            &profile_path(&s.root, &id)?,
            p,
            c.settings.parallel_downloads,
        )
        .await?;
        Ok(())
    }
    .await
    .map_err(err)
}
#[tauri::command]
async fn play(app: tauri::AppHandle, s: tauri::State<'_, State>, id: String) -> Result<(), String> {
    async{
 let permit=s.operation.clone().try_acquire_owned().context("Игра или установка уже запущена")?;s.cancel.store(false,Ordering::Relaxed);let c=s.config.lock().await.clone();let p=c.profiles.iter().find(|p|p.id==id).context("Нет профиля")?;
 let account=c.accounts.iter().find(|a|Some(&a.id)==c.selected_account.as_ref()).context("Добавьте и выберите аккаунт")?;
 let n=net(&s)?;let prepared=engine::prepare(&n,&app,&profile_path(&s.root,&id)?,p,c.settings.parallel_downloads).await?;let access=auth::access(&n,&c.settings.microsoft_client_id,account).await?;
 let mut cmd=engine::launch_command(&prepared,p,account,&access,memory())?;let mut child=cmd.spawn().context("Не удалось запустить Java")?;
 s.running.store(true,Ordering::Relaxed);let _=app.emit("game-state",true);
 let sleep=c.settings.close_on_launch;
 tauri::async_runtime::spawn(async move{
  tokio::time::sleep(std::time::Duration::from_millis(500)).await;
  if sleep{if let Some(w)=app.get_webview_window("main"){let _=w.destroy();}}
  let result=tokio::task::spawn_blocking(move||child.wait()).await;
  let state=app.state::<State>();state.running.store(false,Ordering::Relaxed);drop(permit);
  let message=match result{Ok(Ok(status))if status.success()=>"Игра завершена".to_string(),other=>format!("Игра завершилась с ошибкой: {other:?}. Откройте launcher-game.log в папке сборки.")};
  let _=tokio::fs::write(state.root.join("last-game-result.txt"),&message).await;
  if app.get_webview_window("main").is_none(){let _=tauri::WebviewWindowBuilder::new(&app,"main",tauri::WebviewUrl::App("index.html".into())).title("Aurora Launcher").inner_size(1280.0,820.0).min_inner_size(960.0,680.0).center().build();}
  let _=app.emit("game-state",false);let _=app.emit("game-exited",message);
 });Ok(())
}.await.map_err(err)
}
#[tauri::command]
async fn search_content(
    s: tauri::State<'_, State>,
    profile_id: String,
    provider: String,
    query: String,
    kind: String,
) -> Result<Value, String> {
    let c = s.config.lock().await.clone();
    let p = c
        .profiles
        .iter()
        .find(|p| p.id == profile_id)
        .ok_or("Нет профиля")?;
    mods::search(&net(&s).map_err(err)?, &provider, &query, &kind, p)
        .await
        .map_err(err)
}
#[tauri::command]
async fn install_content(
    app: tauri::AppHandle,
    s: tauri::State<'_, State>,
    profile_id: String,
    provider: String,
    project_id: String,
) -> Result<(), String> {
    async {
        let _permit = s
            .operation
            .clone()
            .try_acquire_owned()
            .context("Дождитесь окончания игры или установки")?;
        s.cancel.store(false, Ordering::Relaxed);
        let c = s.config.lock().await.clone();
        let p = c
            .profiles
            .iter()
            .find(|p| p.id == profile_id)
            .context("Нет профиля")?;
        let dir = profile_path(&s.root, &profile_id)?;
        let n = net(&s)?;
        if provider == "modrinth" {
            if !project_id
                .bytes()
                .all(|b| b.is_ascii_alphanumeric() || b == b'-' || b == b'_')
            {
                bail!("Некорректный ID")
            };
            mods::install_modrinth(&n, &app, &dir, p, &project_id, None, &mut HashSet::new())
                .await?
        } else if provider == "curseforge" {
            mods::install_curseforge(
                &n,
                &app,
                &dir,
                p,
                project_id.parse()?,
                None,
                &mut HashSet::new(),
            )
            .await?
        } else {
            bail!("Неизвестный каталог")
        };
        Ok(())
    }
    .await
    .map_err(err)
}
#[tauri::command]
fn open_login() -> Result<(), String> {
    #[cfg(windows)]
    let result = std::process::Command::new("explorer.exe")
        .arg("https://www.microsoft.com/devicelogin")
        .spawn();
    #[cfg(not(windows))]
    let result = std::process::Command::new("xdg-open")
        .arg("https://www.microsoft.com/devicelogin")
        .spawn();
    result.map(|_| ()).map_err(|e| e.to_string())
}
fn main() {
    tauri::Builder::default()
        .setup(|app| {
            let root = app.path().app_data_dir()?;
            std::fs::create_dir_all(&root)?;
            let config = std::fs::read(root.join("config.json"))
                .ok()
                .and_then(|b| serde_json::from_slice::<Config>(&b).ok())
                .or_else(|| {
                    std::fs::read(root.join("config.bak.json"))
                        .ok()
                        .and_then(|b| serde_json::from_slice(&b).ok())
                })
                .unwrap_or_default();
            app.manage(State {
                root,
                config: Mutex::new(config),
                cancel: Arc::new(AtomicBool::new(false)),
                operation: Arc::new(Semaphore::new(1)),
                running: AtomicBool::new(false),
            });
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            bootstrap,
            versions,
            create_profile,
            set_memory,
            add_offline,
            select_account,
            remove_account,
            save_settings,
            microsoft_begin,
            microsoft_poll,
            cancel,
            install,
            play,
            search_content,
            install_content,
            open_login
        ])
        .build(tauri::generate_context!())
        .expect("Ошибка запуска Aurora")
        .run(|app, event| {
            if let tauri::RunEvent::ExitRequested {
                code: None, api, ..
            } = event
            {
                if app.state::<State>().running.load(Ordering::Relaxed) {
                    api.prevent_exit();
                }
            }
        });
}
