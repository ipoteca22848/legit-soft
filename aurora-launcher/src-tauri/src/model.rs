use serde::{Deserialize, Serialize};
use std::{collections::HashMap, path::{Component,Path,PathBuf}};
use anyhow::{bail,Result};
#[derive(Clone,Serialize,Deserialize)]
#[serde(rename_all="camelCase")]
pub struct Profile { pub id:String,pub name:String,pub version:String,pub loader:String,pub memory_mb:u64,pub performance:bool,pub background:String }
#[derive(Clone,Serialize,Deserialize)]
#[serde(rename_all="camelCase")]
pub struct Account {pub id:String,pub name:String,pub kind:String}
#[derive(Clone,Serialize,Deserialize)]
#[serde(rename_all="camelCase")]
pub struct Settings {pub microsoft_client_id:String,pub close_on_launch:bool,pub parallel_downloads:usize}
impl Default for Settings {fn default()->Self{Self{microsoft_client_id:String::new(),close_on_launch:true,parallel_downloads:8}}}
#[derive(Clone,Serialize,Deserialize,Default)]
#[serde(rename_all="camelCase")]
pub struct Config { pub profiles:Vec<Profile>,pub accounts:Vec<Account>,pub selected_account:Option<String>,pub settings:Settings }
#[derive(Clone,Serialize)]
#[serde(rename_all="camelCase")]
pub struct Progress {pub stage:String,pub current:u64,pub total:u64,pub percent:f64}
#[derive(Clone,Deserialize,Serialize,Default)]
#[serde(rename_all="camelCase")]
pub struct Version {
 pub id:String,#[serde(default)]pub inherits_from:Option<String>,pub main_class:Option<String>,
 #[serde(default)]pub libraries:Vec<Library>,#[serde(default)]pub arguments:Arguments,
 pub minecraft_arguments:Option<String>,pub asset_index:Option<Artifact>,pub assets:Option<String>,pub java_version:Option<JavaVersion>,
 #[serde(default)]pub downloads:HashMap<String,Artifact>,#[serde(rename="type")]pub kind:Option<String>,
}
#[derive(Clone,Deserialize,Serialize,Default)]pub struct Arguments {#[serde(default)]pub game:Vec<serde_json::Value>,#[serde(default)]pub jvm:Vec<serde_json::Value>}
#[derive(Clone,Deserialize,Serialize)]pub struct JavaVersion {#[serde(rename="majorVersion")]pub major_version:u32}
#[derive(Clone,Deserialize,Serialize,Default)]pub struct Artifact {#[serde(default)]pub url:String,pub path:Option<String>,pub sha1:Option<String>,pub id:Option<String>}
#[derive(Clone,Deserialize,Serialize)]pub struct Library {pub name:String,#[serde(default)]pub downloads:LibraryDownloads,#[serde(default)]pub rules:Vec<Rule>,#[serde(default)]pub natives:HashMap<String,String>,pub extract:Option<Extract>,pub url:Option<String>}
#[derive(Clone,Deserialize,Serialize,Default)]pub struct LibraryDownloads {pub artifact:Option<Artifact>,#[serde(default)]pub classifiers:HashMap<String,Artifact>}
#[derive(Clone,Deserialize,Serialize)]pub struct Extract {#[serde(default)]pub exclude:Vec<String>}
#[derive(Clone,Deserialize,Serialize)]pub struct Rule {pub action:String,pub os:Option<Os>,#[serde(default)]pub features:HashMap<String,bool>}
#[derive(Clone,Deserialize,Serialize)]pub struct Os {pub name:Option<String>,pub arch:Option<String>,pub version:Option<String>}
pub fn safe_join(root:&Path,relative:&str)->Result<PathBuf>{
 let portable=relative.replace('\\',"/");
 let path=Path::new(&portable);
 if path.components().any(|c|!matches!(c,Component::Normal(_))) || portable.contains(':') {bail!("Небезопасный путь: {relative}")}
 Ok(root.join(path))
}
pub fn profile_path(root:&Path,id:&str)->Result<PathBuf>{uuid::Uuid::parse_str(id)?;safe_join(&root.join("instances"),id)}
pub fn offline_account(name:&str)->Result<Account>{
 if name.is_empty() || name.len()>16 || !name.bytes().all(|c|c.is_ascii_alphanumeric()||c==b'_'){bail!("Ник: 1–16 латинских букв, цифр или _")}
 let mut bytes=md5::compute(format!("OfflinePlayer:{name}").as_bytes()).0;
 bytes[6]=(bytes[6]&0x0f)|0x30;bytes[8]=(bytes[8]&0x3f)|0x80;
 Ok(Account{id:uuid::Uuid::from_bytes(bytes).simple().to_string(),name:name.into(),kind:"offline".into()})
}
#[cfg(test)]mod tests{use super::*;#[test]fn offline_uuid(){assert_eq!(offline_account("Notch").unwrap().id,"b50ad385829d3141a2167e7d7539ba7f");assert!(offline_account("../bad").is_err());}#[test]fn traversal(){for s in ["../escape","a/../../x","C:\\test","/tmp/x"]{assert!(safe_join(Path::new("data"),s).is_err())}}}
