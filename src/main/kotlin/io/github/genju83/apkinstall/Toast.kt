package io.github.genju83.apkinstall

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import java.io.PrintWriter
import java.io.StringWriter

internal sealed class Toast(private val name: String, private val notificationGroup: NotificationGroup) {
    object Info : Toast(
        "ApkInstall",
        NotificationGroup("info", NotificationDisplayType.NONE, true, null, null)
    )

    object Error : Toast(
        "ApkInstall",
        NotificationGroup("error", NotificationDisplayType.BALLOON, true, null, null)
    )

    fun show(message: String) {
        this.notificationGroup.createNotification(name, message).notify(null)
    }

    fun show(throwable: Throwable?) {
        val stringWriter = StringWriter()
        throwable?.printStackTrace(PrintWriter(stringWriter))
        val exceptionAsString = stringWriter.toString()
        this.notificationGroup.createNotification(name, exceptionAsString).notify(null)
    }
}

interface Notifier {
    fun error(throwable: Throwable?)
    fun info(message: String)
}

object ToastNotifier : Notifier {

    override fun error(throwable: Throwable?) {
        Toast.Error.show(throwable)
    }

    override fun info(message: String) {
        Toast.Info.show(message)
    }

}