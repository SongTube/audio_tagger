
import 'dart:async';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

class AudioTagger {
  static const MethodChannel _channel = MethodChannel('audio_tagger');

  /// Write all (available) Tags into the audio file
  static Future<int> writeAllTags({
    required String songPath,
    required String title,
    required String album,
    required String artist,
    required String genre,
    required String year,
    required String disc,
    required String track
  }) async {
    int result = await _channel.invokeMethod("writeAllTags", {
      "path":   songPath,
      "tagsTitle":  title,
      "tagsAlbum":  album,
      "tagsArtist": artist,
      "tagsGenre":  genre,
      "tagsYear":   year,
      "tagsDisc":   disc,
      "tagsTrack":  track
    });
    return result;
  }

  /// Write artwork using local Image or from Url to specified file path
  static Future<int> writeArtwork({
    required String songPath,
    String? artworkPath,
    String? artworkUrl,
  }) async { 
    if (artworkPath == null && artworkUrl == null) {
      throw Exception('AudioTagger: Both artworkPath and artworkUrl cannot be null');
    }
    if (artworkPath != null) {
      if (!await File(artworkPath).exists()) throw Exception("artworkPath provided path doesnt exist or is invalid");
    }
    late String artwork;
    if (artworkPath == null) {
      final directory = (await getExternalStorageDirectory())!;
      final response = await http.get(Uri.parse(artworkUrl!));
      if (response.statusCode != 200) {
        throw Exception("Error downloading artwork, check your Url or internet connection");
      }
      final artworkFile = await File(directory.path + "/" + _randomString())
        .writeAsBytes(response.bodyBytes);
      artwork = artworkFile.path;
    } else {
      artwork = artworkPath;
    }
    int result = await _channel.invokeMethod("writeArtwork", {
      "path": songPath,
      "artworkPath": artwork
    });
    return result;
  }

  /// Extract the artwork from an audio file
  static Future<Uint8List?> extractArtwork(String filePath) async {
    return await _channel.invokeMethod('extractArtwork', {
      'path': filePath
    });
  }

  /// Generate a Square Cover Image from URL
  static Future<File?> generateCover(String url) async {
    File artwork =
      File((await getApplicationDocumentsDirectory()).path +
        "/${_randomString()}.jpg");
    http.Response response; try {
      response = await http.get(Uri.parse(url));
    } catch (_) {return null;}
    await artwork.writeAsBytes(response.bodyBytes);
    Uint8List? croppedImage = await cropToSquare(artwork);
    if (croppedImage != null) {
      return artwork.writeAsBytes(croppedImage);
    } else {
      return null;
    }
  }

  /// Crop image to Square from File
  static Future<Uint8List?> cropToSquare(File image) async {
    if (await image.exists()) {
      Uint8List? croppedImagePath =
        await _channel.invokeMethod('cropToSquare', {"path": image.path});
      return croppedImagePath;
    }
    return null;
  }

  // Get a random string used
  static String _randomString() {
    const _chars = "abcdefghijklmnopqrstuvwxyz";
    Random _rnd = Random();
    String string = String.fromCharCodes(Iterable.generate(
      6, (_) => _chars.codeUnitAt(_rnd.nextInt(_chars.length))
    ));
    return string;
  }

}
