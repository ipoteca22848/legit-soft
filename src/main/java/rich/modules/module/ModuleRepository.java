package rich.modules.module;
import java.util.List;
public final class ModuleRepository {
 public List<ModuleStructure> modules() {return com.anvil.client.FrontendBridge.modules();}
 public List<ModuleStructure> allModules() {return modules();}
}
