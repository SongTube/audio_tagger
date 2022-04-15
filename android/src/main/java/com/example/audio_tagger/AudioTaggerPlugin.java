package com.example.audio_tagger;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Size;

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
  private static Context context;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "audio_tagger");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    final Map<String, Map<String, String>> resultMessage = new HashMap<>();
    final Map<String, byte[]> bytes = new HashMap<>();
    final Map<String, String> codeResult = new HashMap<>();
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
        try {
          int code = TagsMethods.writeAllTags(
                  path,
                  tagsTitle,
                  tagsAlbum,
                  tagsArtist,
                  tagsGenre,
                  tagsYear,
                  tagsDisc,
                  tagsTrack
          );
          codeResult.put("code", String.valueOf(code));
        } catch (Exception e) {
          codeResult.put("code", String.valueOf(1));
        }
      }

      // Extract all tags from the provided song file
      if (call.method.equals("extractAllTags")) {
        String path = call.argument("path");
        try {
          Map<String, String> tags = TagsMethods.extractAllTags(path);
          resultMessage.put("tags", tags);
        } catch (Exception e) {
          resultMessage.put("tags", null);
        }
      }

      // Write down only the artwork on the provided song file
      if (call.method.equals("writeArtwork")) {
        String path = call.argument("path");
        String artwork = call.argument("artworkPath");
        int code;
        try {
          code = TagsMethods.writeArtwork(
                  path,
                  artwork
          );
        } catch (Exception e) {
          code = 1;
          codeResult.put("code", String.valueOf(code));
        }
        codeResult.put("code", String.valueOf(code));
      }

      // Extract the artwork from the provided audio file
      if (call.method.equals("extractArtwork")) {
        String path = call.argument("path");
        try {
          bytes.put("bytes", TagsMethods.extractArtwork(path));
        } catch (Exception e) {
          bytes.put("bytes", new byte[0]);
        }
      }

      // Extract the thumbnail from the provided audio file
      if (call.method.equals("extractThumbnail")) {
        String path = call.argument("path");
        try {
          bytes.put("bytes", TagsMethods.extractThumbnail(path));
        } catch (Exception e) {
          bytes.put("bytes", new byte[0]);
        }
      }

      // Crop any image file to a square
      if (call.method.equals("cropToSquare")) {
        try {
          String path = call.argument("path");
          Bitmap bitmap = BitmapFactory.decodeFile(path);
          Bitmap croppedBitmap = cropToSquare(bitmap);
          ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
          croppedBitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
          bytes.put("bytes", byteStream.toByteArray());
        } catch (Exception e) {
          bytes.put("bytes", new byte[0]);
        }
      }
      handler.post(() -> {
        if (call.method.equals("extractArtwork") || call.method.equals("extractThumbnail")) {
          result.success(bytes.get("bytes"));
        } else if (call.method.equals("cropToSquare")) {
          result.success(bytes.get("bytes"));
        } else if (call.method.equals("extractAllTags")) {
          result.success(resultMessage.get("tags"));
        } else {
          result.success(Integer.parseInt(codeResult.get("code")));
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
        return 1;
      } catch (IOException e) {
        return 1;
      } catch (CannotWriteException e) {
        return 1;
      } catch (TagException e) {
        return 1;
      } catch (ReadOnlyFileException e) {
        return 1;
      } catch (InvalidAudioFrameException e) {
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
        return null;
      } catch (IOException e) {
        return null;
      } catch (TagException e) {
        return null;
      } catch (ReadOnlyFileException e) {
        return null;
      } catch (InvalidAudioFrameException e) {
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
        return 1;
      } catch (IOException e) {
        return 1;
      } catch (CannotWriteException e) {
        return 1;
      } catch (TagException e) {
        return 1;
      } catch (ReadOnlyFileException e) {
        return 1;
      } catch (InvalidAudioFrameException e) {
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
        return null;
      } catch (IOException e) {
        return null;
      } catch (TagException e) {
        return null;
      } catch (ReadOnlyFileException e) {
        return null;
      } catch (InvalidAudioFrameException e) {
        return null;
      } catch (NullPointerException e) {
        return null;
      }
    }

    static byte[] extractThumbnail(String path) {
      File file = new File(path);
      try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          Bitmap image = ThumbnailUtils.createAudioThumbnail(file, new Size(200, 200), null);
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
          return stream.toByteArray();
        } else {
          MediaMetadataRetriever mmr = new MediaMetadataRetriever();
          mmr.setDataSource(context, Uri.fromFile(file));
          byte[] bytes = mmr.getEmbeddedPicture();
          if (bytes == null) {
            return new byte[0];
          } else {
            return bytes;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return new byte[0];
    }

  }

}
