## v1.0.2 – 05/10/2025

### Changed
* Moved MemoryUtils into processor
* Moved IProcessorListener to its own file
* Moved MemoryOptions into ProcessorData.kt
* Update indexers to users correctly named parameter item instead of id to prevent issues with named parameters

### Fixed
* Fixed typo in getScaledDimension function

## v1.0.1 – 26/09/2025

### Changed
* IEmbeddingStore interface - getAll, isCached, exist 
* Use linked hashmap for cache instead of list
* Pass store to Indexers
* Update tests

## v1.0.0 – 23/09/2025
* Initial release