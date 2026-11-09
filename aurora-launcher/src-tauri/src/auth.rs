use crate::{download::Net, model::Account};
use anyhow::{bail, Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
#[derive(Serialize, Deserialize)]
pub struct Tokens {
    pub refresh_token: String,
}
pub fn entry(id: &str) -> Result<keyring::Entry> {
    Ok(keyring::Entry::new("AuroraLauncher", id)?)
}
pub async fn begin(net: &Net, client: &str) -> Result<Value> {
    if client.trim().is_empty() {
        bail!("Укажите Microsoft Client ID в настройках")
    };
    Ok(net
        .client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode")
        .form(&[
            ("client_id", client),
            ("scope", "XboxLive.signin offline_access"),
        ])
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?)
}
pub async fn poll(net: &Net, client: &str, device: &str) -> Result<Value> {
    let response: Value = net
        .client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .form(&[
            ("client_id", client),
            ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
            ("device_code", device),
        ])
        .send()
        .await?
        .json()
        .await?;
    if let Some(e) = response["error"].as_str() {
        if e == "authorization_pending" || e == "slow_down" {
            return Ok(json!({"pending":true,"slow":e=="slow_down"}));
        }
        bail!("Microsoft: {e}")
    }
    let (account, _) = exchange(net, &response).await?;
    Ok(json!({"pending":false,"account":account}))
}
async fn exchange(net: &Net, oauth: &Value) -> Result<(Account, String)> {
    let msa = oauth["access_token"]
        .as_str()
        .context("Нет Microsoft access_token")?;
    let xbl:Value=net.client.post("https://user.auth.xboxlive.com/user/authenticate").json(&json!({"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":format!("d={msa}")},"RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"})).send().await?.error_for_status()?.json().await?;
    let xtoken = xbl["Token"].as_str().context("Xbox token")?;
    let xsts:Value=net.client.post("https://xsts.auth.xboxlive.com/xsts/authorize").json(&json!({"Properties":{"SandboxId":"RETAIL","UserTokens":[xtoken]},"RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"})).send().await?.error_for_status().context("XSTS: проверьте Xbox-профиль и разрешения семейного аккаунта")?.json().await?;
    let uhs = xsts["DisplayClaims"]["xui"][0]["uhs"]
        .as_str()
        .context("Xbox user hash")?;
    let xt = xsts["Token"].as_str().context("XSTS token")?;
    let mc: Value = net
        .client
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&json!({"identityToken":format!("XBL3.0 x={uhs};{xt}")}))
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?;
    let access = mc["access_token"]
        .as_str()
        .context("Minecraft access token")?;
    let ent: Value = net
        .client
        .get("https://api.minecraftservices.com/entitlements/mcstore")
        .bearer_auth(access)
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?;
    if !ent["items"].as_array().is_some_and(|v| {
        v.iter()
            .any(|i| i["name"] == "game_minecraft" || i["name"] == "product_minecraft")
    }) {
        bail!("На аккаунте не найдена лицензия Minecraft Java")
    }
    let profile: Value = net
        .client
        .get("https://api.minecraftservices.com/minecraft/profile")
        .bearer_auth(access)
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?;
    let account = Account {
        id: profile["id"].as_str().context("UUID Minecraft")?.into(),
        name: profile["name"].as_str().context("Ник Minecraft")?.into(),
        kind: "microsoft".into(),
    };
    let refresh = oauth["refresh_token"]
        .as_str()
        .context("Нет refresh_token")?;
    entry(&account.id)?.set_password(&serde_json::to_string(&Tokens {
        refresh_token: refresh.into(),
    })?)?;
    Ok((account, access.into()))
}
pub async fn access(net: &Net, client: &str, account: &Account) -> Result<String> {
    if account.kind == "offline" {
        return Ok("0".into());
    }
    let saved: Tokens = serde_json::from_str(
        &entry(&account.id)?
            .get_password()
            .context("Войдите в Microsoft повторно")?,
    )?;
    let oauth: Value = net
        .client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .form(&[
            ("client_id", client),
            ("grant_type", "refresh_token"),
            ("refresh_token", &saved.refresh_token),
            ("scope", "XboxLive.signin offline_access"),
        ])
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?;
    let (fresh, token) = exchange(net, &oauth).await?;
    if fresh.id != account.id {
        bail!("UUID аккаунта изменился; войдите заново")
    };
    Ok(token)
}
