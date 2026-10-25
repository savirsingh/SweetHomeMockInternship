# Implementation and verification

## Run on this Mac

Java 8 (Azul Zulu for Apple Silicon) is installed. Double-click `launch.command`, or run it in a terminal. The script selects Java 8 on macOS before launching the supported Maven entry point.

The equivalent terminal command from the repository root is:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn compile exec:java
```

The assignment intentionally disables the 3D view. An empty 3D area is expected.

## Task 2: editable descriptions — issue #1

`HomeFurnitureController.isPropertyEditable` now allows `DESCRIPTION`; price remains read-only and selection-dependent properties keep their existing behavior. The nearby documentation describes those exceptions. The existing dialog bindings, `modifyFurniture`, and undo support already transfer description edits to the selected furniture, so no second persistence path was introduced.

Additional checks edit a description, undo and redo it, and serialize/reload the home to verify the text survives.

## Task 3: calculated volume — issue #2

`PieceOfFurniture` exposes `getVolume`. Catalog and home furniture calculate it from `getWidth() * getDepth() * getHeight()`, including updated dimensions and furniture groups. There is no stored volume that could become stale.

`HomePieceOfFurniture.SortableProperty.VOLUME` has a comparator. New homes display Volume after Height. `FurnitureController` inserts a re-enabled column in that same order. `FurnitureTable` supplies its header, preferred width, existing length-number renderer, and CSV/clipboard output. Existing dimension-change listeners refresh sorting when furniture is resized.

`HomeView`, `HomeController`, and `HomePane` define, enable, and connect the sort/display actions. Both the Furniture menu and table context menu reuse these action maps. English labels, mnemonics, and tooltip strings are in `swing/package.properties`. Existing macOS menu decorators intentionally suppress visible tooltips; the underlying actions contain them.

Existing saved homes retain their stored visible-column preferences. Use **Furniture > Display column > Volume** when opening the supplied older sample.

## Task 4: all-level printing — issue #3

`PlanComponent.print` maps each page to a viewable level in `Home.getLevels()` order, fits its plan onto one page, and restores the original selected level and furniture selection in `finally`. Empty viewable levels receive a blank plan page. Homes without explicit levels keep the existing print scale and tiling behavior.

`FurnitureTable` creates a separate printable column model. It appends Level only when absent, prints all viewable-level rows even when the screen has a filter, and restores the original columns, filter, grid color, and selection in `finally`. `HomePrintableComponent` no longer restricts furniture printing to the selected level. The existing coordinator still emits furniture first and plans afterward.

The sample PDF contains four pages: the all-level table, Level 0, Level 1, and Level 2. The screen's Level-column preference is unchanged.

## Checks

```sh
mvn clean test
./verification/run.sh
python3 test_submission.py
```

The provided `assignment-tests` are unchanged. `verification/FeatureRegression.java` adds standalone checks without changing the assignment test configuration. They cover description persistence and undo/redo, catalog volume, sorting and resizing, CSV output, column ordering, repeated and out-of-order printing, hidden/empty levels, null selections, injected render/print failures, all-level furniture rows, exactly one printed Level column, and restoration of screen state and selection.

Acceptance artifacts and their capture details are listed in `submissions/README.md`.

Review the changed classes and these checks before submitting so you can explain the implementation, as required by the assignment's AI policy.
