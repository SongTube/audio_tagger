# audio_tagger

A Flutter (Android only) plugin for editing and extracting information from your music,
at the moment it only works on AAC and OGG (last one only support setting and extracting tags)

## Getting Started

Just call the function you need from *AudioTagger* class and pass the music file path and data.
These are some examples of what this plugin can do:

```dart

// Write all provided Tags to an audio file
AudioTagger.writeAllTags(
    songPath: ...
    tags: ...
)

// Extract all tags from an audio file as an [AudioTags] object
AudioTagger.extractAllTags(...songPath)

// Writes the provided artwork from path or url to the specified audio file path
// This will write either from path or url, if both are provided, path takes priority
// This also throws exception if both path or url are null
AudioTagger.writeArtwork(
    songPath: ...
    artworkPath: ...
    artworkUrl: ...
)

// Extracts the artwork from the provided audio file at full quality, returns a [Uint8List] object
AudioTagger.extractArtwork(...filePath)

// Generate a Square Cover Image from provided URL and returns a [File] object
AudioTagger.generateCover(...url)

// Crops an image to 4:3 aspect ratio and returns an [Uint8List] object
AudioTagger.cropToSquare(...fileImage)

```

