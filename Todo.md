n-cube 'ToDo' list
======

### n-cube engine

* Datatypes
 * Additional datatypes: list, map, set, array containing only JSON primitive types
 * DefaultCellValue needs to be declared and processed like the cell type and requiredScopeKeys
* OptionalScope
 * Support additional (and subtractive) optional scope keys (minus sign (-) in front will remove an optional scope key)
 * Test all regular expression patterns to ensure they find cube names
 * Docs on optional scope: mention that optional scope will not be found in URL GroovyExpression cells


### n-cube editor (NCE)
* Push / Pull support for move cubes between two persistent storage locations.
* Search / Filter support
  * filter app names (with drop-down type matching)
  * filter cube names (with drop-down type matching)
  * search cubes (including axis names, column, and cell values) for string

// TODO: Where does cell menu go? (for cut/copy/paste)
// TODO: Implement optional keys (with minus sign support)
// TODO: test all regex's related to finding referenced cubes
// TODO: test CellInfo (in preparation for list, array, set, map)
// TODO: create visualization of n-cube in NCE
