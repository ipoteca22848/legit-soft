package rich.modules.module;
import lombok.Getter;
import lombok.Setter;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.SettingRepository;
import rich.util.animations.*;
@Getter @Setter
public class ModuleStructure extends SettingRepository {
 private final String name, description;
 private final ModuleCategory category;
 private final Animation animation = new Decelerate().setMs(175).setValue(1);
 private int key = -1, type = 1;
 public boolean state, favorite;
 public ModuleStructure(String name, String description, ModuleCategory category) {
  this.name=name;this.description=description;this.category=category;
 }
 public void switchState() {setState(!isState());}
 public void setState(boolean enabled) {state=enabled;animation.setDirection(enabled?Direction.FORWARDS:Direction.BACKWARDS);}
 public void switchFavorite() {favorite=!favorite;}
}
