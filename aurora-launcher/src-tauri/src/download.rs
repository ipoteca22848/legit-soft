use crate::model::Progress;
use anyhow::{bail, Context, Result};
use futures_util::StreamExt;
use sha1::Digest;
use std::{
    path::{Path, PathBuf},
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc,
    },
};
use tauri::{AppHandle, Emitter};
use tokio::io::AsyncWriteExt;
#[derive(Clone)]
pub struct Net {
    pub client: reqwest::Client,
    pub cancel: Arc<AtomicBool>,
}
impl Net {
    pub fn new(cancel: Arc<AtomicBool>) -> Result<Self> {
        Ok(Self {
            client: reqwest::Client::builder()
                .user_agent("AuroraLauncher/0.1.1 (github.com/ipoteca22848/legit-soft)")
                .connect_timeout(std::time::Duration::from_secs(20))
                .timeout(std::time::Duration::from_secs(600))
                .build()?,
            cancel,
        })
    }
    pub fn check(&self) -> Result<()> {
        if self.cancel.load(Ordering::Relaxed) {
            bail!("Операция отменена")
        }
        Ok(())
    }
    pub async fn json(&self, url: &str) -> Result<serde_json::Value> {
        self.check()?;
        Ok(self
            .client
            .get(url)
            .send()
            .await?
            .error_for_status()?
            .json()
            .await?)
    }
    pub async fn file(&self, url: &str, path: &Path, hash: Option<(&str, &str)>) -> Result<()> {
        self.check()?;
        if path.is_file() && hash.is_some() && verify(path, hash).await? {
            return Ok(());
        }
        let parsed = reqwest::Url::parse(url)?;
        if parsed.scheme() != "https" {
            bail!("Разрешены только HTTPS-загрузки")
        }
        tokio::fs::create_dir_all(path.parent().context("Нет каталога")?).await?;
        let temp = path.with_extension(format!("{}.part", uuid::Uuid::new_v4()));
        let mut last = None;
        for attempt in 0..3 {
            let result: Result<()> = async {
                let response = self.client.get(url).send().await?.error_for_status()?;
                let mut stream = response.bytes_stream();
                let mut out = tokio::fs::File::create(&temp).await?;
                while let Some(chunk) = stream.next().await {
                    self.check()?;
                    out.write_all(&chunk?).await?;
                }
                out.flush().await?;
                drop(out);
                if !verify(&temp, hash).await? {
                    bail!("Контрольная сумма не совпадает: {}", path.display())
                }
                if path.exists() {
                    tokio::fs::remove_file(path).await?
                }
                tokio::fs::rename(&temp, path).await?;
                Ok(())
            }
            .await;
            match result {
                Ok(()) => return Ok(()),
                Err(e) => {
                    last = Some(e);
                    let _ = tokio::fs::remove_file(&temp).await;
                    self.check()?;
                    if attempt < 2 {
                        tokio::time::sleep(std::time::Duration::from_millis(700 * (attempt + 1)))
                            .await;
                    }
                }
            }
        }
        Err(last.unwrap()).context(format!("Загрузка {}", path.display()))
    }
}
async fn verify(path: &Path, hash: Option<(&str, &str)>) -> Result<bool> {
    let Some((kind, expected)) = hash else {
        return Ok(true);
    };
    let p = path.to_owned();
    let kind = kind.to_owned();
    let expected = expected.to_owned();
    tokio::task::spawn_blocking(move || -> Result<bool> {
        use std::io::Read;
        let mut f = std::fs::File::open(p)?;
        let mut buf = [0u8; 65536];
        macro_rules! digest {
            ($t:ty) => {{
                let mut d = <$t>::new();
                loop {
                    let n = f.read(&mut buf)?;
                    if n == 0 {
                        break;
                    }
                    d.update(&buf[..n]);
                }
                hex::encode(d.finalize())
            }};
        }
        let actual = match kind.as_str() {
            "sha1" => digest!(sha1::Sha1),
            "sha256" => digest!(sha2::Sha256),
            "sha512" => digest!(sha2::Sha512),
            _ => bail!("Неизвестный хеш"),
        };
        Ok(actual.eq_ignore_ascii_case(&expected))
    })
    .await?
}
pub fn progress(app: &AppHandle, stage: &str, current: u64, total: u64) {
    let _ = app.emit(
        "install-progress",
        Progress {
            stage: stage.into(),
            current,
            total,
            percent: if total == 0 {
                0.0
            } else {
                100.0 * current as f64 / total as f64
            },
        },
    );
}
#[derive(Clone)]
pub struct Job {
    pub url: String,
    pub path: PathBuf,
    pub sha1: Option<String>,
}
pub async fn batch(
    net: &Net,
    app: &AppHandle,
    stage: &str,
    jobs: Vec<Job>,
    parallel: usize,
) -> Result<()> {
    let total = jobs.len() as u64;
    progress(app, stage, 0, total);
    let mut stream = futures_util::stream::iter(jobs.into_iter().map(|j| {
        let net = net.clone();
        async move {
            net.file(&j.url, &j.path, j.sha1.as_deref().map(|h| ("sha1", h)))
                .await
        }
    }))
    .buffer_unordered(parallel.clamp(1, 16));
    let mut done = 0;
    while let Some(r) = stream.next().await {
        r?;
        done += 1;
        progress(app, stage, done, total)
    }
    Ok(())
}
pub fn unzip(archive: &Path, target: &Path, exclude: &[String]) -> Result<()> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(archive)?)?;
    for i in 0..zip.len() {
        let mut f = zip.by_index(i)?;
        let Some(rel) = f.enclosed_name() else {
            bail!("Путь вне ZIP")
        };
        if exclude.iter().any(|e| f.name().starts_with(e))
            || f.unix_mode().is_some_and(|m| m & 0o170000 == 0o120000)
        {
            continue;
        }
        let path = target.join(rel);
        if f.is_dir() {
            std::fs::create_dir_all(path)?;
        } else {
            std::fs::create_dir_all(path.parent().unwrap())?;
            let mut out = std::fs::File::create(path)?;
            std::io::copy(&mut f, &mut out)?;
        }
    }
    Ok(())
}
