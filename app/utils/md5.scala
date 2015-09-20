package utils

import java.security.MessageDigest

object md5 {

  def apply(s: String): String =
    MessageDigest.getInstance("MD5").digest(s.getBytes)
      .map("%02X".format(_)).mkString.toLowerCase
}
