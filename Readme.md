# Monomovie

Simple website to bookmark, select and display offers by streaming services for movies.
Uses JustWatch as the data source, especially for the information regarding streaming services.

### Features

Currently, there is only ever one global user. There is no extension to multiple users planned

- Bookmark movies
- Watch history
- List all streaming services offering a movie (for all countries)
- Jellyfin integration
- choose a random movie for watching from a weighted list of movies


## Debugging

If you are debugging monomovie, make sure to enable the Ktor development mode.
IDEA should do this automatically, but even if it says it did, it may be useful
to manually add `-Dio.ktor.development=true` to your VM arguments.

Without setting this, you loose out on some useful debugging utilities,
and your dev server might not even start.
