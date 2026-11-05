package rich;
public final class Initialization {
 private static final Initialization INSTANCE = new Initialization();
 private final rich.manager.Manager manager = new rich.manager.Manager();
 public static Initialization getInstance() { return INSTANCE; }
 public rich.manager.Manager getManager() { return manager; }
}
