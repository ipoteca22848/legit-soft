package rich.modules.module.category;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    HUD("HUD"), CONFIGS("Конфиги"), MARKS("Метки"), COSMETICS("Косметика"), SWORDS("Мечи"),
    THEMES("Темы"), EVENTS("Ивенты"), MINES("Шахты"), MESSENGER("Мессенджер"), SETTINGS("Настройки");

    final String readableName;
}