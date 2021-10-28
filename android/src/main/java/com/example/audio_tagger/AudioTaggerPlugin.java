package com.example.audio_tagger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** AudioTaggerPlugin */
public class AudioTaggerPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "audio_tagger");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    final Map<String, Map<String, String>> resultMessage = new HashMap<>();
    final Map<String, byte[]> bytes = new HashMap<>();
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Handler handler = new Handler(Looper.getMainLooper());
    executor.execute(() -> {

      // Write down all tags on the provided song file
      if (call.method.equals("writeAllTags")) {
        String path = call.argument("path");
        String tagsTitle = call.argument("tagsTitle");
        String tagsAlbum = call.argument("tagsAlbum");
        String tagsArtist = call.argument("tagsArtist");
        String tagsGenre = call.argument("tagsGenre");
        String tagsYear = call.argument("tagsYear");
        String tagsDisc = call.argument("tagsDisc");
        String tagsTrack = call.argument("tagsTrack");
        result.success(TagsMethods.writeAllTags(
                path,
                tagsTitle,
                tagsAlbum,
                tagsArtist,
                tagsGenre,
                tagsYear,
                tagsDisc,
                tagsTrack
        ));
      }

      // Extract all tags from the provided song file
      if (call.method.equals("extractAllTags")) {
        String path = call.argument("path");
        resultMessage.put("tags", TagsMethods.extractAllTags(path));
      }

      // Write down only the artwork on the provided song file
      if (call.method.equals("writeArtwork")) {
        String path = call.argument("path");
        String artwork = call.argument("artworkPath");
        result.success(TagsMethods.writeArtwork(
                path,
                artwork
        ));
      }

      // Extract the artwork from the provided audio file
      if (call.method.equals("extractArtwork")) {
        String path = call.argument("path");
        bytes.put("bytes", TagsMethods.extractArtwork(path));
      }

      // Crop any image file to a square
      if (call.method.equals("cropToSquare")) {
        String path = call.argument("path");
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        Bitmap croppedBitmap = cropToSquare(bitmap);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
        bytes.put("bytes", byteStream.toByteArray());
      }
      handler.post(new Runnable() {
        @Override
        public void run() {
          if (call.method.equals("extractArtwork")) {
            result.success(bytes.get("bytes"));
          }
          if (call.method.equals("cropToSquare")) {
            result.success(bytes.get("bytes"));
          }
          if (call.method.equals("extractAllTags")) {
            result.success(resultMessage.get("tags"));
          }
        }
      });
    });
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  private Bitmap cropToSquare(Bitmap image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int newWidth = Math.min(height, width);
    int newHeight = (height > width) ? height - (height - width) : height;
    int cropW = (width - height) / 2;
    cropW = Math.max(cropW, 0);
    int cropH = (height - width) / 2;
    cropH = Math.max(cropH, 0);
    return Bitmap.createBitmap(image, cropW, cropH, newWidth, newHeight);
  }

  static class TagsMethods {

    static int writeAllTags(
            String path,
            String title,
            String album,
            String artist,
            String genre,
            String year,
            String disc,
            String track
    ) {
      File file = new File(path);
      try {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        if (title != null) tag.setField(FieldKey.TITLE, title);
        if (album != null) tag.setField(FieldKey.ALBUM, album);
        if (artist != null) tag.setField(FieldKey.ARTIST, artist);
        if (genre != null) tag.setField(FieldKey.GENRE, genre);
        if (year != null) tag.setField(FieldKey.YEAR, year);
        if (disc != null) tag.setField(FieldKey.DISC_NO, disc);
        if (track != null) tag.setField(FieldKey.TRACK, track);
        audioFile.commit();
        return 0;
      } catch (CannotReadException e) {
        e.printStackTrace();
        return 1;
      } catch (IOException e) {
        e.printStackTrace();
        return 1;
      } catch (CannotWriteException e) {
        e.printStackTrace();
        return 1;
      } catch (TagException e) {
        e.printStackTrace();
        return 1;
      } catch (ReadOnlyFileException e) {
        e.printStackTrace();
        return 1;
      } catch (InvalidAudioFrameException e) {
        e.printStackTrace();
        return 1;
      }
    }

    static Map<String, String> extractAllTags(String path) {
      File file = new File(path);
      try {
        AudioFile audioFile = AudioFileIO.read(file);
        Map<String, String> map = new HashMap<>();
        map.put("title", audioFile.getTag().getFirst(FieldKey.TITLE));
        map.put("album", audioFile.getTag().getFirst(FieldKey.ALBUM));
        map.put("artist", audioFile.getTag().getFirst(FieldKey.ARTIST));
        map.put("genre", audioFile.getTag().getFirst(FieldKey.GENRE));
        map.put("year", audioFile.getTag().getFirst(FieldKey.YEAR));
        map.put("disc", audioFile.getTag().getFirst(FieldKey.DISC_NO));
        map.put("track", audioFile.getTag().getFirst(FieldKey.TRACK));
        return map;
      } catch (CannotReadException e) {
        e.printStackTrace();
        return null;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } catch (TagException e) {
        e.printStackTrace();
        return null;
      } catch (ReadOnlyFileException e) {
        e.printStackTrace();
        return null;
      } catch (InvalidAudioFrameException e) {
        e.printStackTrace();
        return null;
      }
    }

    static int writeArtwork(
            String path,
            String artworkPath
    ) {
      File file = new File(path);
      File artworkFile = new File(artworkPath);
      try {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        Artwork artwork = ArtworkFactory.createArtworkFromFile(artworkFile);
        tag.setField(artwork);
        audioFile.commit();
        return 0;
      } catch (CannotReadException e) {
        e.printStackTrace();
        return 1;
      } catch (IOException e) {
        e.printStackTrace();
        return 1;
      } catch (CannotWriteException e) {
        e.printStackTrace();
        return 1;
      } catch (TagException e) {
        e.printStackTrace();
        return 1;
      } catch (ReadOnlyFileException e) {
        e.printStackTrace();
        return 1;
      } catch (InvalidAudioFrameException e) {
        e.printStackTrace();
        return 1;
      }
    }

    static byte[] extractArtwork(String path) {
      File file = new File(path);
      try {
        AudioFile audioFile = AudioFileIO.read(file);
        Artwork artwork = audioFile.getTag().getFirstArtwork();
        return artwork.getBinaryData();
      } catch (CannotReadException e) {
        e.printStackTrace();
        return null;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } catch (TagException e) {
        e.printStackTrace();
        return null;
      } catch (ReadOnlyFileException e) {
        e.printStackTrace();
        return null;
      } catch (InvalidAudioFrameException e) {
        e.printStackTrace();
        return null;
      }
    }

  }

}
