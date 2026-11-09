import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { invoke, isTauri } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import {
  Home,
  Blocks,
  Settings,
  Users,
  Plus,
  Play,
  Download,
  ChevronDown,
  ArrowUpRight,
  Check,
  LoaderCircle,
  X,
  Search,
  SlidersHorizontal,
  ShieldCheck,
  Monitor,
  Leaf,
  Folder,
  CheckCircle2,
  LogOut,
  ChevronRight,
  Mountain,
  MemoryStick,
  ExternalLink,
  Square,
  AlertCircle,
} from "lucide-react";
import "./styles.css";
type Profile = {
  id: string;
  name: string;
  version: string;
  loader: string;
  memoryMb: number;
  performance: boolean;
  background: string;
};
type Account = { id: string; name: string; kind: string };
type Preferences = {
  microsoftClientId: string;
  closeOnLaunch: boolean;
  parallelDownloads: number;
};
type Config = {
  profiles: Profile[];
  accounts: Account[];
  selectedAccount: string | null;
  settings: Preferences;
};
type Boot = {
  config: Config;
  memoryMb: number;
  dataDirectory: string;
  running: boolean;
  lastGameResult?: string | null;
};
type Progress = {
  stage: string;
  current: number;
  total: number;
  percent: number;
};
type Hit = {
  id: string;
  title: string;
  description: string;
  icon?: string;
  downloads: number;
};
type Device = {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
  created: number;
};
const empty: Config = {
  profiles: [],
  accounts: [],
  selectedAccount: null,
  settings: {
    microsoftClientId: "",
    closeOnLaunch: true,
    parallelDownloads: 8,
  },
};
const tabs = [
  { id: "home", label: "Главная", icon: Home },
  { id: "mods", label: "Контент", icon: Blocks },
  { id: "settings", label: "Настройки", icon: Settings },
  { id: "accounts", label: "Аккаунты", icon: Users },
];
const loaderNames: Record<string, string> = {
  vanilla: "Vanilla",
  fabric: "Fabric",
  forge: "Forge",
  quilt: "Quilt",
};
function App() {
  const [config, setConfig] = useState<Config>(empty),
    [hardware, setHardware] = useState(8192),
    [dataDir, setDataDir] = useState(""),
    [tab, setTab] = useState("home"),
    [selected, setSelected] = useState(""),
    [busy, setBusy] = useState(false),
    [running, setRunning] = useState(false),
    [progress, setProgress] = useState<Progress | null>(null),
    [notice, setNotice] = useState<{ text: string; error: boolean } | null>(
      null,
    ),
    [modal, setModal] = useState(false),
    [versionList, setVersionList] = useState([
      "1.21.1",
      "1.20.1",
      "1.19.4",
      "1.18.2",
      "1.16.5",
    ]);
  const [name, setName] = useState("Новый мир"),
    [version, setVersion] = useState("1.21.1"),
    [loader, setLoader] = useState("fabric"),
    [performance, setPerformance] = useState(true),
    [background, setBackground] = useState("");
  const [ram, setRam] = useState(4096),
    [nick, setNick] = useState(""),
    [prefs, setPrefs] = useState(empty.settings),
    [cfKey, setCfKey] = useState(""),
    [device, setDevice] = useState<Device | null>(null);
  const [provider, setProvider] = useState("modrinth"),
    [kind, setKind] = useState("mod"),
    [query, setQuery] = useState(""),
    [hits, setHits] = useState<Hit[]>([]),
    [searching, setSearching] = useState(false),
    [searched, setSearched] = useState(false);
  const reduced = useReducedMotion();
  const searchSeq = useRef(0);
  const tauri = isTauri();
  const profile =
    config.profiles.find((p) => p.id === selected) || config.profiles[0];
  const account = config.accounts.find((a) => a.id === config.selectedAccount);
  async function refresh(initial = false) {
    if (!tauri) return;
    const b = await invoke<Boot>("bootstrap");
    setConfig(b.config);
    setPrefs(b.config.settings);
    setHardware(b.memoryMb);
    setDataDir(b.dataDirectory);
    setRunning(b.running);
    if(initial && b.lastGameResult) notify(b.lastGameResult,!b.lastGameResult.startsWith("Игра завершена"));
  }
  function notify(text: string, error = false) {
    setNotice({ text, error });
  }
  async function action<T>(fn: () => Promise<T>, success?: string) {
    if (busy) return;
    setBusy(true);
    setProgress(null);
    try {
      const result = await fn();
      await refresh();
      if (success) notify(success);
      return result;
    } catch (e) {
      notify(String(e), true);
    } finally {
      setBusy(false);
      setProgress(null);
    }
  }
  useEffect(() => {
    if (!tauri) {
      notify(
        "Предпросмотр интерфейса. Установка и запуск доступны в приложении .exe.",
      );
      return;
    }
    refresh(true).catch((e) => notify(String(e), true));
    invoke<string[]>("versions")
      .then((v) => {
        if (v.length) setVersionList(v);
      })
      .catch(() => {});
    const unsubs = [
      listen<Progress>("install-progress", (e) => setProgress(e.payload)),
      listen<boolean>("game-state", (e) => setRunning(e.payload)),
      listen<string>("game-exited", (e) =>
        notify(e.payload, !e.payload.startsWith("Игра завершена")),
      ),
    ];
    return () => {
      unsubs.forEach((p) => {
        p.then((fn) => fn()).catch(() => {});
      });
    };
  }, []);
  useEffect(() => {
    if (profile) setRam(profile.memoryMb);
  }, [profile?.id, profile?.memoryMb]);
  useEffect(() => {
    if (!notice) return;
    const id = setTimeout(() => setNotice(null), notice.error ? 16000 : 6500);
    return () => clearTimeout(id);
  }, [notice]);
  useEffect(() => {
    if (!device) return;
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    let interval = Math.max(device.interval || 5, 5) * 1000;
    const poll = async () => {
      if (stopped) return;
      if (Date.now() > device.created + device.expires_in * 1000) {
        setDevice(null);
        notify("Код Microsoft истёк. Начните вход заново.", true);
        return;
      }
      try {
        const r = await invoke<{ pending: boolean; slow?: boolean }>(
          "microsoft_poll",
          { deviceCode: device.device_code },
        );
        if (stopped) return;
        if (!r.pending) {
          setDevice(null);
          await refresh();
          notify("Microsoft-аккаунт подключён");
          return;
        }
        if (r.slow) interval += 5000;
      } catch (e) {
        if (!stopped) {
          setDevice(null);
          notify(String(e), true);
        }
        return;
      }
      if (!stopped) timer = setTimeout(poll, interval);
    };
    timer = setTimeout(poll, interval);
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [device]);
  async function create() {
    await action(async () => {
      const p = await invoke<Profile>("create_profile", {
        name,
        version,
        loader,
        memoryMb: Math.min(4096, Math.max(1024, hardware - 2048)),
        performance: performance && loader !== "vanilla",
        background,
      });
      setSelected(p.id);
      setModal(false);
    }, "Профиль создан. Нажмите «Играть», чтобы установить игру.");
  }
  async function saveRam() {
    if (profile && ram !== profile.memoryMb)
      await action(() =>
        invoke("set_memory", { id: profile.id, memoryMb: ram }),
      );
  }
  async function search() {
    if (!profile) {
      notify("Сначала создайте профиль", true);
      return;
    }
    const seq = ++searchSeq.current;
    setSearching(true);
    setSearched(true);
    try {
      const r = await invoke<Hit[]>("search_content", {
        profileId: profile.id,
        provider,
        query,
        kind,
      });
      if (seq === searchSeq.current) setHits(r);
    } catch (e) {
      if (seq === searchSeq.current) {
        setHits([]);
        notify(String(e), true);
      }
    } finally {
      if (seq === searchSeq.current) setSearching(false);
    }
  }
  const operationLabel = progress
    ? progress.stage
    : busy
      ? "Подготовка…"
      : running
        ? "Игра запущена"
        : "Играть";
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#"
          onClick={(e) => {
            e.preventDefault();
            setTab("home");
          }}
        >
          <span className="brand-mark">
            <Mountain size={25} strokeWidth={2.4} />
          </span>
          <span>
            AURORA<small>MINECRAFT LAUNCHER</small>
          </span>
        </a>
        <div className="nav-caption">ВАШЕ ПРОСТРАНСТВО</div>
        <nav>
          {tabs.map((t) => (
            <button
              key={t.id}
              className={`nav-item ${tab === t.id ? "active" : ""}`}
              onClick={() => setTab(t.id)}
            >
              <t.icon size={19} />
              <span>{t.label}</span>
              {tab === t.id && <span className="nav-dot" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-profiles">
          <div className="nav-caption">
            МОИ СБОРКИ{" "}
            <button
              className="icon-button"
              title="Создать сборку"
              onClick={() => setModal(true)}
            >
              <Plus size={15} />
            </button>
          </div>
          {config.profiles.length ? (
            config.profiles.map((p, i) => (
              <button
                key={p.id}
                className={`mini-profile ${profile?.id === p.id ? "chosen" : ""}`}
                onClick={() => {
                  setSelected(p.id);
                  setTab("home");
                }}
              >
                <span className={`profile-icon tone-${i % 3}`}>
                  <Blocks size={17} />
                </span>
                <span>
                  {p.name}
                  <small>
                    {p.version} · {loaderNames[p.loader]}
                  </small>
                </span>
              </button>
            ))
          ) : (
            <p className="sidebar-empty">
              Ваш следующий мир
              <br />
              начинается здесь.
            </p>
          )}
        </div>
        <div className="sidebar-footer">
          <div className="edition">
            <span className="status-dot" />
            JAVA EDITION<span>v0.1.2</span>
          </div>
          <button className="account-card" onClick={() => setTab("accounts")}>
            <span className="avatar">
              {account?.name.slice(0, 2).toUpperCase() || "+"}
            </span>
            <span>
              {account?.name || "Добавить аккаунт"}
              <small>
                {account
                  ? account.kind === "offline"
                    ? "Offline-аккаунт"
                    : "Microsoft · Java Edition"
                  : "Выберите способ входа"}
              </small>
            </span>
            <ChevronRight size={15} />
          </button>
        </div>
      </aside>
      <main>
        <header className="topbar">
          <div className="breadcrumb">
            Лаунчер <ChevronRight size={13} />
            <strong>{tabs.find((t) => t.id === tab)?.label}</strong>
          </div>
          <div className="top-actions">
            <span className="preview-badge">PREVIEW</span>
            <button
              className="icon-button"
              onClick={() => setTab("settings")}
              title="Настройки"
            >
              <SlidersHorizontal size={18} />
            </button>
          </div>
        </header>
        <AnimatePresence mode="wait">
          <motion.div
            key={tab}
            initial={{ opacity: 0, y: reduced ? 0 : 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: reduced ? 0 : -5 }}
            transition={{ duration: reduced ? 0 : 0.17 }}
            className="page"
          >
            {tab === "home" && (
              <>
                <div className="page-heading">
                  <div>
                    <div className="eyebrow">
                      РАДЫ ВАС ВИДЕТЬ
                      {account ? `, ${account.name.toUpperCase()}` : ""}
                    </div>
                    <h1>
                      Время для нового мира<span>.</span>
                    </h1>
                  </div>
                  <button className="secondary" onClick={() => setModal(true)}>
                    <Plus size={16} />
                    Новая сборка
                  </button>
                </div>
                <section
                  className={`hero landscape-${config.profiles.indexOf(profile!) % 3}`}
                >
                  {profile?.background && !reduced ? (
                    <video
                      key={profile.id}
                      className="hero-video"
                      src={profile.background}
                      autoPlay
                      loop
                      muted
                      playsInline
                      onError={() =>
                        notify(
                          "Видео недоступно. Используется стандартный фон.",
                        )
                      }
                    />
                  ) : null}
                  <div className="hero-shade" />
                  <div className="hero-top">
                    <span className="glass-pill">
                      <span className="status-dot" />
                      {profile ? "ВЫБРАННАЯ СБОРКА" : "ВАША СЛЕДУЮЩАЯ ИСТОРИЯ"}
                    </span>
                    <span className="hero-coordinate">
                      OVERWORLD <span>↗</span>
                    </span>
                  </div>
                  <div className="hero-content">
                    <div className="version-tags">
                      <span>MINECRAFT {profile?.version || "JAVA"}</span>
                      {profile && <span>{loaderNames[profile.loader]}</span>}
                    </div>
                    <h2>
                      {profile?.name || (
                        <>
                          За горизонтом
                          <br />
                          ждёт ваш мир.
                        </>
                      )}
                    </h2>
                    <p>
                      {profile
                        ? profile.performance
                          ? "Больше кадров. Больше открытий. Ваша сборка готова к приключениям."
                          : "Стройте, исследуйте и создавайте свою историю."
                        : "Создайте первую сборку. Всё необходимое мы подготовим за вас."}
                    </p>
                    <div className="hero-bottom">
                      <button
                        className={`play-button ${busy ? "is-busy" : ""}`}
                        disabled={busy || running || !tauri}
                        onClick={() => {
                          if (!profile) {
                            setModal(true);
                            return;
                          }
                          if (!account) {
                            setTab("accounts");
                            notify("Добавьте аккаунт для запуска");
                            return;
                          }
                          action(() => invoke("play", { id: profile.id }));
                        }}
                      >
                        <span
                          className="play-fill"
                          style={{ width: `${progress?.percent || 0}%` }}
                        />
                        <span className="play-inner">
                          {busy ? (
                            <LoaderCircle className="spin" size={21} />
                          ) : (
                            <Play size={21} fill="currentColor" />
                          )}
                          <span>
                            {profile ? operationLabel : "Создать сборку"}
                          </span>
                          {progress && (
                            <strong>{Math.round(progress.percent)}%</strong>
                          )}
                        </span>
                      </button>
                      {busy ? (
                        <button
                          className="hero-link"
                          onClick={() =>
                            invoke("cancel").catch((e) =>
                              notify(String(e), true),
                            )
                          }
                        >
                          <Square size={13} />
                          Отменить
                        </button>
                      ) : (
                        <span className="launch-note">
                          <ShieldCheck size={15} />
                          {profile
                            ? "Изолированный профиль"
                            : "Отдельная папка для каждого мира"}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="hero-number">
                    01 <span>/ YOUR WORLD</span>
                  </div>
                </section>
                <div className="dashboard-grid">
                  <section className="panel ram-panel">
                    <div className="panel-title">
                      <span className="soft-icon">
                        <MemoryStick size={19} />
                      </span>
                      <div>
                        <h3>Память для игры</h3>
                        <p>Оставьте место для большого мира</p>
                      </div>
                      <strong>
                        {(ram / 1024).toFixed(ram % 1024 ? 1 : 0)}{" "}
                        <small>ГБ</small>
                      </strong>
                    </div>
                    <input
                      aria-label="Выделенная оперативная память"
                      type="range"
                      min={1024}
                      max={Math.max(
                        1024,
                        Math.floor((hardware - 2048) / 512) * 512,
                      )}
                      step={512}
                      value={ram}
                      disabled={!profile || busy || running}
                      onChange={(e) => setRam(+e.target.value)}
                      onPointerUp={saveRam}
                      onKeyUp={saveRam}
                    />
                    <div className="range-labels">
                      <span>1 ГБ</span>
                      <span>
                        Всего в системе: {(hardware / 1024).toFixed(0)} ГБ
                      </span>
                    </div>
                  </section>
                  <section className="panel performance-panel">
                    <div className="panel-title">
                      <span className="soft-icon">
                        <Leaf size={19} />
                      </span>
                      <div>
                        <h3>Легче. Быстрее. Плавнее.</h3>
                        <p>
                          {profile?.performance
                            ? "Performance Pack включён"
                            : "Оптимизация под вашу сборку"}
                        </p>
                      </div>
                    </div>
                    <div className="performance-bottom">
                      <span>
                        {profile?.loader === "forge" ? "Embeddium" : "Sodium"}{" "}
                        <span className="muted">+ зависимости</span>
                      </span>
                      <span
                        className={profile?.performance ? "enabled" : "muted"}
                      >
                        {profile?.performance ? (
                          <>
                            <Check size={13} />
                            АВТО
                          </>
                        ) : (
                          "При создании"
                        )}
                      </span>
                    </div>
                  </section>
                </div>
                <div className="section-heading">
                  <h3>Ваши приключения</h3>
                  <span>{config.profiles.length} сборок</span>
                </div>
                <div className="profiles-grid">
                  {config.profiles.slice(0, 5).map((p, i) => (
                    <button
                      key={p.id}
                      className={`profile-card ${profile?.id === p.id ? "selected" : ""}`}
                      onClick={() => setSelected(p.id)}
                    >
                      <span className={`profile-art tone-${i % 3}`}>
                        <Mountain size={34} />
                      </span>
                      <span className="profile-info">
                        <strong>{p.name}</strong>
                        <small>
                          {p.version} · {loaderNames[p.loader]}
                        </small>
                      </span>
                      {profile?.id === p.id ? (
                        <CheckCircle2 size={17} className="mint" />
                      ) : (
                        <ChevronRight size={17} />
                      )}
                    </button>
                  ))}
                  <button
                    className="new-profile"
                    onClick={() => setModal(true)}
                  >
                    <Plus size={21} />
                    <span>Создать сборку</span>
                  </button>
                </div>
                <footer className="page-footer">
                  <span>
                    <span className="status-dot" />
                    Ваш мир. Ваши правила.
                  </span>
                  <span>Неофициальный лаунчер Minecraft Java Edition</span>
                </footer>
              </>
            )}
            {tab === "mods" && (
              <>
                <div className="page-heading">
                  <div>
                    <div className="eyebrow">СОБЕРИТЕ СВОЙ МИР</div>
                    <h1>
                      Меньше ограничений<span>.</span>
                    </h1>
                  </div>
                  <select
                    aria-label="Профиль для контента"
                    value={profile?.id || ""}
                    onChange={(e) => setSelected(e.target.value)}
                  >
                    {!profile && <option value="">Создайте профиль</option>}
                    {config.profiles.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="content-toolbar panel">
                  <div className="segmented">
                    <button
                      className={provider === "modrinth" ? "on" : ""}
                      onClick={() => {
                        setProvider("modrinth");
                        setHits([]);
                        setSearched(false);
                      }}
                    >
                      Modrinth
                    </button>
                    <button
                      className={provider === "curseforge" ? "on" : ""}
                      onClick={() => {
                        setProvider("curseforge");
                        setHits([]);
                        setSearched(false);
                      }}
                    >
                      CurseForge
                    </button>
                  </div>
                  <select
                    aria-label="Тип контента"
                    value={kind}
                    onChange={(e) => {
                      setKind(e.target.value);
                      setHits([]);
                      setSearched(false);
                    }}
                  >
                    <option value="mod">Моды</option>
                    <option value="resourcepack">Ресурспаки</option>
                    <option value="shader">Шейдеры</option>
                  </select>
                  <form
                    className="search-form"
                    onSubmit={(e) => {
                      e.preventDefault();
                      search();
                    }}
                  >
                    <Search size={17} />
                    <input
                      placeholder="Название или идея…"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                    />
                    <button disabled={searching || !profile || !tauri}>
                      {searching ? (
                        <LoaderCircle size={17} className="spin" />
                      ) : (
                        "Найти"
                      )}
                    </button>
                  </form>
                </div>
                <p className="helper">
                  Контент подбирается для{" "}
                  {profile
                    ? `${profile.version} · ${loaderNames[profile.loader]}`
                    : "выбранного профиля"}
                  . Обязательные зависимости загружаются автоматически. Для
                  шейдеров нужен совместимый Iris / Oculus.
                </p>
                <div className="content-grid">
                  {hits.map((h) => (
                    <article className="panel content-card" key={h.id}>
                      {h.icon ? (
                        <img
                          src={h.icon}
                          alt=""
                          loading="lazy"
                          onError={(e) =>
                            (e.currentTarget.style.display = "none")
                          }
                        />
                      ) : (
                        <span className="soft-icon">
                          <Blocks />
                        </span>
                      )}
                      <div>
                        <h3>{h.title}</h3>
                        <p>{h.description}</p>
                        <small>
                          {Intl.NumberFormat("ru", {
                            notation: "compact",
                          }).format(h.downloads)}{" "}
                          загрузок
                        </small>
                      </div>
                      <button
                        className="icon-button"
                        title={`Установить ${h.title}`}
                        disabled={busy || running}
                        onClick={() =>
                          action(
                            () =>
                              invoke("install_content", {
                                profileId: profile!.id,
                                provider,
                                projectId: h.id,
                              }),
                            `${h.title}: установлено`,
                          )
                        }
                      >
                        <Download size={19} />
                      </button>
                    </article>
                  ))}
                </div>
                {!hits.length && (
                  <div className="empty-state">
                    <Blocks size={40} />
                    <h2>
                      {searched
                        ? "Ничего не найдено"
                        : "Небольшое изменение. Новый опыт."}
                    </h2>
                    <p>
                      {searched
                        ? "Попробуйте другой запрос или проверьте выбранную версию."
                        : "Найдите моды, ресурспаки и шейдеры для вашей сборки."}
                    </p>
                  </div>
                )}
                {busy && progress && (
                  <div className="inline-progress">
                    <LoaderCircle className="spin" size={16} />
                    {progress.stage}
                  </div>
                )}
              </>
            )}
            {tab === "accounts" && (
              <>
                <div className="page-heading">
                  <div>
                    <div className="eyebrow">ВАША ЛИЧНОСТЬ В ИГРЕ</div>
                    <h1>
                      Каждое имя — история<span>.</span>
                    </h1>
                  </div>
                  <ShieldCheck className="mint" size={27} />
                </div>
                <div className="two-col">
                  <section className="panel form-panel">
                    <div className="soft-icon">
                      <Monitor size={23} />
                    </div>
                    <h2>Microsoft</h2>
                    <p>
                      Вход через официальный сайт Microsoft. Для аккаунтов с
                      Minecraft Java Edition.
                    </p>
                    <button
                      className="primary"
                      disabled={busy || !!device || !tauri}
                      onClick={() =>
                        action(async () => {
                          const d =
                            await invoke<Omit<Device, "created">>(
                              "microsoft_begin",
                            );
                          setDevice({ ...d, created: Date.now() });
                        })
                      }
                    >
                      <ShieldCheck size={17} />
                      Войти через Microsoft
                      <ArrowUpRight size={16} />
                    </button>
                    {device && (
                      <div className="device-box">
                        <p>Откройте {device.verification_uri} и введите код:</p>
                        <strong>{device.user_code}</strong>
                        <button
                          className="secondary"
                          onClick={() =>
                            invoke("open_login").catch((e) =>
                              notify(String(e), true),
                            )
                          }
                        >
                          Открыть Microsoft
                          <ExternalLink size={15} />
                        </button>
                        <small>
                          <LoaderCircle size={13} className="spin" />
                          Ожидаем подтверждение…
                        </small>
                        <button
                          className="text-button"
                          onClick={() => setDevice(null)}
                        >
                          Отменить вход
                        </button>
                      </div>
                    )}
                    <small>
                      Перед первым входом укажите свой Microsoft Client ID в
                      настройках.
                    </small>
                  </section>
                  <section className="panel form-panel">
                    <div className="soft-icon">
                      <Users size={23} />
                    </div>
                    <h2>Offline-аккаунт</h2>
                    <p>
                      Для одиночной игры и серверов, разрешающих
                      offline-подключения. UUID создаётся по никнейму.
                    </p>
                    <form
                      onSubmit={(e) => {
                        e.preventDefault();
                        action(async () => {
                          await invoke("add_offline", { name: nick });
                          setNick("");
                        }, "Offline-аккаунт добавлен");
                      }}
                    >
                      <label>
                        НИКНЕЙМ
                        <input
                          placeholder="Steve"
                          minLength={1}
                          maxLength={16}
                          pattern="[A-Za-z0-9_]+"
                          value={nick}
                          onChange={(e) => setNick(e.target.value)}
                          required
                        />
                      </label>
                      <button className="secondary" disabled={busy || !tauri}>
                        <Plus size={17} />
                        Добавить аккаунт
                      </button>
                    </form>
                    <small>
                      Offline-вход не предоставляет доступ к серверам с
                      online-mode.
                    </small>
                  </section>
                </div>
                <div className="section-heading">
                  <h3>Ваши аккаунты</h3>
                  <span>{config.accounts.length}</span>
                </div>
                {config.accounts.map((a) => (
                  <div key={a.id} className="panel account-row">
                    <span className="avatar">
                      {a.name.slice(0, 2).toUpperCase()}
                    </span>
                    <div>
                      <strong>{a.name}</strong>
                      <small>
                        {a.kind === "offline" ? "Offline" : "Microsoft"} ·{" "}
                        {a.id}
                      </small>
                    </div>
                    <button
                      className="secondary"
                      disabled={busy || config.selectedAccount === a.id}
                      onClick={() =>
                        action(() => invoke("select_account", { id: a.id }))
                      }
                    >
                      {config.selectedAccount === a.id ? (
                        <>
                          <Check size={15} />
                          Выбран
                        </>
                      ) : (
                        "Выбрать"
                      )}
                    </button>
                    <button
                      className="icon-button"
                      title="Удалить аккаунт из лаунчера"
                      disabled={busy}
                      onClick={() =>
                        action(() => invoke("remove_account", { id: a.id }))
                      }
                    >
                      <LogOut size={17} />
                    </button>
                  </div>
                ))}
              </>
            )}
            {tab === "settings" && (
              <>
                <div className="page-heading">
                  <div>
                    <div className="eyebrow">ВСЁ ПОД ВАШИМ КОНТРОЛЕМ</div>
                    <h1>
                      Точность в деталях<span>.</span>
                    </h1>
                  </div>
                </div>
                <section className="panel settings-panel">
                  <h3>Производительность</h3>
                  <div className="setting-row">
                    <div>
                      <strong>Освобождать память при запуске</strong>
                      <p>
                        Закрывать WebView на время игры и восстанавливать окно
                        после выхода.
                      </p>
                    </div>
                    <button
                      role="switch"
                      aria-checked={prefs.closeOnLaunch}
                      aria-label="Освобождать память"
                      className={`toggle ${prefs.closeOnLaunch ? "on" : ""}`}
                      onClick={() =>
                        setPrefs({
                          ...prefs,
                          closeOnLaunch: !prefs.closeOnLaunch,
                        })
                      }
                    >
                      <span />
                    </button>
                  </div>
                  <div className="setting-row">
                    <div>
                      <strong>Параллельные загрузки</strong>
                      <p>Одновременно загружаемые библиотеки и ассеты.</p>
                    </div>
                    <select
                      aria-label="Число загрузок"
                      value={prefs.parallelDownloads}
                      onChange={(e) =>
                        setPrefs({
                          ...prefs,
                          parallelDownloads: +e.target.value,
                        })
                      }
                    >
                      {[1, 2, 4, 8, 12, 16].map((n) => (
                        <option key={n}>{n}</option>
                      ))}
                    </select>
                  </div>
                  <h3>Подключения</h3>
                  <label>
                    MICROSOFT APPLICATION (CLIENT) ID
                    <input
                      placeholder="ID вашего приложения в Microsoft Entra"
                      value={prefs.microsoftClientId}
                      onChange={(e) =>
                        setPrefs({
                          ...prefs,
                          microsoftClientId: e.target.value,
                        })
                      }
                    />
                  </label>
                  <p className="helper">
                    Приложение должно разрешать public client / device code flow
                    и иметь доступ к Minecraft Services API. Client secret не
                    нужен.
                  </p>
                  <label>
                    CURSEFORGE API KEY
                    <input
                      type="password"
                      autoComplete="off"
                      placeholder="Введите ключ — хранится в системном хранилище"
                      value={cfKey}
                      onChange={(e) => setCfKey(e.target.value)}
                    />
                  </label>
                  <p className="helper">
                    Пустое поле сохраняет существующий ключ. Токены Microsoft
                    также хранятся в Windows Credential Manager.
                  </p>
                  <button
                    className="primary"
                    disabled={busy || !tauri}
                    onClick={() =>
                      action(async () => {
                        await invoke("save_settings", {
                          settings: prefs,
                          curseforgeKey: cfKey || null,
                        });
                        setCfKey("");
                      }, "Настройки сохранены")
                    }
                  >
                    <Check size={17} />
                    Сохранить настройки
                  </button>
                </section>
                <section className="panel path-panel">
                  <Folder size={21} />
                  <div>
                    <h3>Изолированные сборки</h3>
                    <p>
                      {dataDir
                        ? `${dataDir}\\instances\\<UUID>`
                        : "%APPDATA%\\dev.aurora.minecraft.launcher\\instances\\<UUID>"}
                    </p>
                    <small>
                      Миры, моды, настройки, Java, библиотеки и assets находятся
                      внутри каждой сборки.
                    </small>
                  </div>
                </section>
              </>
            )}
          </motion.div>
        </AnimatePresence>
      </main>
      <AnimatePresence>
        {notice && (
          <motion.div
            role={notice.error ? "alert" : "status"}
            className={`toast ${notice.error ? "error" : ""}`}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
          >
            {notice.error ? (
              <AlertCircle size={19} />
            ) : (
              <CheckCircle2 size={19} />
            )}
            <span>{notice.text}</span>
            <button
              className="icon-button"
              onClick={() => setNotice(null)}
              aria-label="Закрыть уведомление"
            >
              <X size={15} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>
      {modal && (
        <div
          className="modal-backdrop"
          onClick={(e) => {
            if (e.target === e.currentTarget && !busy) setModal(false);
          }}
        >
          <motion.section
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-title"
            initial={{ opacity: 0, scale: reduced ? 1 : 0.97 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <div className="modal-heading">
              <span className="eyebrow">НАЧАЛО ПРИКЛЮЧЕНИЯ</span>
              <button
                className="icon-button"
                onClick={() => setModal(false)}
                aria-label="Закрыть"
              >
                <X size={20} />
              </button>
            </div>
            <h2 id="create-title">
              Создайте свой мир<span>.</span>
            </h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                create();
              }}
            >
              <label>
                НАЗВАНИЕ СБОРКИ
                <input
                  autoFocus
                  required
                  maxLength={60}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </label>
              <div className="two-col">
                <label>
                  ВЕРСИЯ MINECRAFT
                  <select
                    value={version}
                    onChange={(e) => setVersion(e.target.value)}
                  >
                    {versionList.map((v) => (
                      <option key={v}>{v}</option>
                    ))}
                  </select>
                </label>
                <label>
                  ЗАГРУЗЧИК
                  <select
                    value={loader}
                    onChange={(e) => setLoader(e.target.value)}
                  >
                    {Object.entries(loaderNames).map(([k, v]) => (
                      <option key={k} value={k}>
                        {v}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={performance && loader !== "vanilla"}
                  disabled={loader === "vanilla"}
                  onChange={(e) => setPerformance(e.target.checked)}
                />
                <span>
                  Performance Pack
                  <small>
                    {loader === "forge" ? "Embeddium" : "Sodium"} + обязательные
                    зависимости. Наличие зависит от версии.
                  </small>
                </span>
                <Leaf size={18} />
              </label>
              <label>
                ВИДЕОФОН · НЕОБЯЗАТЕЛЬНО
                <input
                  type="url"
                  placeholder="https://example.com/background.webm"
                  value={background}
                  onChange={(e) => setBackground(e.target.value)}
                />
              </label>
              <p className="helper">
                Прямая HTTPS-ссылка на .mp4 или .webm. Без видео используется
                встроенный пейзаж.
              </p>
              <button className="primary full" disabled={busy || !tauri}>
                <Plus size={18} />
                {busy ? "Создание…" : "Создать сборку"}
              </button>
            </form>
          </motion.section>
        </div>
      )}
    </div>
  );
}
createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
