class AudioTags {

  AudioTags({
    required this.title,
    required this.album,
    required this.artist,
    required this.genre,
    required this.year,
    required this.disc,
    required this.track
  });

  final String title;
  final String album;
  final String artist;
  final String genre;
  final String year;
  final String disc;
  final String track;

  static AudioTags fromMap(Map<String, dynamic> map) {
    return AudioTags(
      title:  map['title'],
      album:  map['album'],
      artist: map['artist'],
      genre:  map['genre'],
      year:   map['year'],
      disc:   map['disc'],
      track:  map['track']
    );
  }

}