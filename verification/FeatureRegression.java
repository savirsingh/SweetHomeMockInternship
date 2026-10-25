import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import com.eteks.sweethome3d.io.*;
import com.eteks.sweethome3d.model.*;
import com.eteks.sweethome3d.swing.*;
import com.eteks.sweethome3d.viewcontroller.*;

/** Additional regression checks; leaves the provided assignment-tests untouched. */
public class FeatureRegression {
  static int checks;
  static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
    checks++;
  }
  static HomePieceOfFurniture piece(String name, float width, float depth, float height) {
    return new HomePieceOfFurniture(new CatalogPieceOfFurniture(name, null, null,
        width, depth, height, true, false));
  }
  static PageFormat page() {
    Paper paper = new Paper();
    paper.setSize(612, 792);
    paper.setImageableArea(36, 36, 540, 720);
    PageFormat format = new PageFormat();
    format.setPaper(paper);
    return format;
  }
  static BufferedImage image() {
    return new BufferedImage(800, 1000, BufferedImage.TYPE_INT_RGB);
  }
  static DefaultUserPreferences preferences() {
    DefaultUserPreferences prefs = new DefaultUserPreferences();
    prefs.setLanguage("en");
    prefs.setUnit(LengthUnit.CENTIMETER);
    return prefs;
  }
  static void description() throws Exception {
    Home home = new Home();
    HomePieceOfFurniture item = piece("Chair", 2, 3, 4);
    home.addPieceOfFurniture(item);
    home.setSelectedItems(Collections.singletonList(item));
    UndoableEditSupport support = new UndoableEditSupport();
    UndoManager undo = new UndoManager();
    support.addUndoableEditListener(undo);
    HomeFurnitureController controller = new HomeFurnitureController(home, preferences(),
        new SwingViewFactory(), support);
    String old = item.getDescription();
    controller.setDescription("Edited description");
    controller.modifyFurniture();
    check("Edited description".equals(item.getDescription()), "Description saved on furniture");
    undo.undo();
    check(Objects.equals(old, item.getDescription()), "Description undo");
    undo.redo();
    check("Edited description".equals(item.getDescription()), "Description redo");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ObjectOutputStream(bytes).writeObject(home);
    Home restored = (Home)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    check("Edited description".equals(restored.getFurniture().get(0).getDescription()), "Description serialization");
  }
  static void volume() throws Exception {
    CatalogPieceOfFurniture catalog = new CatalogPieceOfFurniture("Box", null, null, 2, 3, 4, true, false);
    check(catalog.getVolume() == 24, "Catalog volume");
    Home home = new Home();
    HomePieceOfFurniture small = new HomePieceOfFurniture(catalog);
    HomePieceOfFurniture large = piece("Large", 3, 4, 5);
    home.addPieceOfFurniture(large);
    home.addPieceOfFurniture(small);
    FurnitureController controller = new FurnitureController(home, preferences(), new SwingViewFactory());
    FurnitureTable table = (FurnitureTable)controller.getView();
    controller.toggleFurnitureSort(HomePieceOfFurniture.SortableProperty.VOLUME);
    check(table.getValueAt(0,0) == small, "Ascending volume table order");
    controller.toggleFurnitureSortOrder();
    check(table.getValueAt(0,0) == large, "Descending volume table order");
    small.setWidth(10);
    check(small.getVolume() == 120, "Volume updates with resized dimensions");
    check(table.getValueAt(0,0) == small, "Table re-sorts after resize");
    StringWriter csv = new StringWriter();
    table.exportToCSV(csv, ',');
    check(csv.toString().contains("Volume"), "Volume CSV header");
    controller.toggleFurnitureVisibleProperty(HomePieceOfFurniture.SortableProperty.VOLUME);
    check(!home.getFurnitureVisibleProperties().contains(HomePieceOfFurniture.SortableProperty.VOLUME), "Hide volume");
    controller.toggleFurnitureVisibleProperty(HomePieceOfFurniture.SortableProperty.VOLUME);
    check(home.getFurnitureVisibleProperties().indexOf(HomePieceOfFurniture.SortableProperty.VOLUME)
        == home.getFurnitureVisibleProperties().indexOf(HomePieceOfFurniture.SortableProperty.HEIGHT)+1, "Column insertion order");
  }
  static Home levels() {
    Home home = new Home();
    for (int i=0; i<3; i++) {
      Level level = new Level("Level " + i, i*250, 10, 240);
      home.addLevel(level);
      Wall wall = new Wall(0, 0, 100+i*100, 0, 10, 240);
      wall.setLevel(level);
      home.addWall(wall);
      HomePieceOfFurniture furniture = piece("Level "+i+" furniture", 20, 30, 40);
      home.setSelectedLevel(level);
      home.addPieceOfFurniture(furniture);
    }
    return home;
  }
  static void printing() throws Exception {
    Home home = levels();
    Level original = home.getSelectedLevel();
    java.util.List<Level> visited = new ArrayList<>();
    PlanComponent plan = new PlanComponent(home, preferences(), null) {
      public float getPrintPreferredScale(Graphics g, PageFormat p) {
        visited.add(home.getSelectedLevel());
        return super.getPrintPreferredScale(g,p);
      }
    };
    java.util.List<Selectable> selection = Collections.singletonList(home.getFurniture().get(2));
    home.setSelectedItems(selection);
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, e -> home.setSelectedItems(Collections.emptyList()));
    Graphics2D graphics = image().createGraphics();
    try {
      for (int i : new int[]{2,0,1,0}) {
        check(plan.print(graphics, page(), i) == Printable.PAGE_EXISTS, "Repeated/out-of-order level page "+i);
        check(home.getSelectedLevel() == original, "Selected level restored after each page");
        check(visited.get(visited.size()-1) == home.getLevels().get(i), "Correct level order");
        check(home.getSelectedItems().equals(selection), "Furniture selection restored after level listeners");
      }
      check(plan.print(graphics, page(), -1) == Printable.NO_SUCH_PAGE, "Reject negative page");
      home.getLevels().get(1).setViewable(false);
      check(plan.print(graphics, page(), 1) == Printable.PAGE_EXISTS, "Hidden level skipped");
      check(visited.get(visited.size()-1) == original, "Hidden level page maps to next viewable level");
      check(plan.print(graphics, page(), 2) == Printable.NO_SUCH_PAGE, "Only viewable level pages");
      PlanComponent broken = new PlanComponent(home, preferences(), null) {
        public float getPrintPreferredScale(Graphics g, PageFormat p) {
          throw new IllegalStateException("Injected renderer failure");
        }
      };
      boolean failed = false;
      try { broken.print(graphics, page(), 0); } catch (IllegalStateException expected) { failed = true; }
      check(failed && home.getSelectedLevel() == original, "Restore level after renderer failure");
      home.setSelectedLevel(null);
      plan.print(graphics, page(), 0);
      check(home.getSelectedLevel() == null, "Restore null selected level");
      FurnitureTable table = new FurnitureTable(home, preferences()) {
        public Printable getPrintable(PrintMode mode, java.text.MessageFormat header,
                                       java.text.MessageFormat footer) {
          return (g, format, index) -> {
            check(getRowCount() == 2, "Printed table includes all viewable levels despite UI filter");
            Set<Level> printedLevels = new HashSet<>();
            for (int row=0; row<getRowCount(); row++) {
              printedLevels.add(((HomePieceOfFurniture)getValueAt(row,0)).getLevel());
            }
            check(printedLevels.contains(home.getLevels().get(0))
                && printedLevels.contains(home.getLevels().get(2)), "Printed furniture spans Level 0 and Level 2");
            int levelColumns = 0;
            for (int column=0; column<getColumnCount(); column++) {
              if (getColumnModel().getColumn(column).getIdentifier() == HomePieceOfFurniture.SortableProperty.LEVEL) levelColumns++;
            }
            check(levelColumns == 1, "Exactly one print-only Level column");
            if (format.getImageableWidth() == 0) throw new PrinterException("Injected print failure");
            return Printable.PAGE_EXISTS;
          };
        }
      };
      FurnitureTable.FurnitureFilter filter = (h,p) -> p.getLevel() == original;
      table.setFurnitureFilter(filter);
      TableColumnModel columns = table.getColumnModel();
      Color grid = table.getGridColor();
      java.util.List<HomePieceOfFurniture.SortableProperty> visible = new ArrayList<>(home.getFurnitureVisibleProperties());
      check(table.print(graphics,page(),0) == Printable.PAGE_EXISTS, "Table print succeeds");
      check(table.getColumnModel() == columns && table.getFurnitureFilter() == filter,
          "Restore table state after successful print");
      Paper tiny = new Paper(); tiny.setSize(100,100); tiny.setImageableArea(0,0,0,0);
      PageFormat invalid = new PageFormat(); invalid.setPaper(tiny);
      try { table.print(graphics, invalid, 0); } catch (PrinterException expected) { }
      check(table.getColumnModel() == columns, "Restore table columns after failed print");
      check(table.getFurnitureFilter() == filter, "Restore furniture filter after failed print");
      check(table.getGridColor().equals(grid), "Restore table grid color");
      check(home.getFurnitureVisibleProperties().equals(visible), "No on-screen Level column mutation");
      java.util.List<HomePieceOfFurniture.SortableProperty> withLevel = new ArrayList<>(visible);
      withLevel.add(HomePieceOfFurniture.SortableProperty.LEVEL);
      home.setFurnitureVisibleProperties(withLevel);
      TableColumnModel withLevelColumns = table.getColumnModel();
      table.print(graphics,page(),0);
      check(home.getFurnitureVisibleProperties().equals(withLevel)
          && table.getColumnModel() == withLevelColumns, "Keep pre-existing on-screen Level column");
      home.getLevels().get(0).setViewable(false);
      home.getLevels().get(2).setViewable(false);
      check(plan.print(graphics,page(),0) == Printable.NO_SUCH_PAGE, "No pages for unviewable levels");
      Home empty = new Home();
      empty.addLevel(new Level("Empty",0,10,240));
      PlanComponent emptyPlan = new PlanComponent(empty, preferences(), null);
      check(emptyPlan.print(graphics,page(),0) == Printable.PAGE_EXISTS, "Empty viewable level has a page");
      check(emptyPlan.print(graphics,page(),1) == Printable.NO_SUCH_PAGE, "Empty level has only one page");
    } finally { graphics.dispose(); }
  }
  public static void main(String[] args) throws Exception {
    System.setProperty("com.eteks.sweethome3d.no3D","true");
    SwingUtilities.invokeAndWait(() -> {
      try { description(); volume(); printing(); }
      catch (Exception ex) { throw new RuntimeException(ex); }
    });
    System.out.println("Additional regression checks passed: " + checks);
    System.exit(0);
  }
}
