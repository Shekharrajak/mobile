package org.fedoraproject.mobile

import Implicits._

import android.content.Intent
import android.net.Uri
import android.os.{ Bundle, Environment }
import android.util.Log
import android.widget.Toast

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ File, BufferedOutputStream, FileOutputStream }
import java.net.{ HttpURLConnection, URL }

class DownloadHeadActivity extends TypedActivity {

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.download_head_activity)

    Log.v("DownloadHeadActivity", s"Starting download")

    // Make sure we have a download directory
    val path = if (Environment.getExternalStorageState == Environment.MEDIA_MOUNTED) {
      new File(Environment.getExternalStorageDirectory + "/download/")
    } else {
      new File(getFilesDir, "/download/")
    }
    path.mkdirs()

    val file = new File(path, "fedora-mobile-0.1.apk")
    if (file.exists) file.delete()

    // TODO: Use Task instead.
    // TODO: Stop pattern matching on ADT constructors.
    Task {
      val connection = new URL(
        "http://da.gd/fmsnap")
        .openConnection
        .asInstanceOf[HttpURLConnection]
      val urlStream = connection.getInputStream
      val outputStream = new BufferedOutputStream(new FileOutputStream(file))
      Iterator
        .continually(urlStream.read)
        .takeWhile(_ != -1)
        .foreach(outputStream.write)
      outputStream.flush
    }.runAsync(_.fold(
      err => {
        runOnUiThread(Toast.makeText(this, R.string.update_failure, Toast.LENGTH_LONG).show)
        Log.e("DownloadHeadActivity", err.toString)
        ()
      },
      _ => {
        val intent = new Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        startActivity(intent)
      }
    ))
  }
}
