## v1.0.4 – 19/10/2025

### Changed

* Pass file directly in `FileEmbeddingStore` constructor instead of dir and filename
* Update batch processor to ensure progress is tracked correctly regardless of errors
* Update batch processor to call onComplete even if items is empty

## v1.0.3 – 14/10/2025

### Added

* `FileEmbeddingRetriever` now supports batch retrieval via `start` and `end` indices with a new `query` overload.
* `FileEmbeddingStore` `getAll` method renamed to `get`, and two new overloads added:
    * `get(ids: List<Long>)` – fetch multiple embeddings by ID.
    * `get(id: Long)` – fetch a single embedding by ID.
* Tests added to verify correct behavior and boundary handling for the new query overload.

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