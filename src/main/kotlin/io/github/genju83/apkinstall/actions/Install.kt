package io.github.genju83.apkinstall.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.Presentation
import io.github.genju83.apkinstall.AdbBridge

class Install : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        AdbBridge.Install.execute(e.project, e)
    }

    override fun update(e: AnActionEvent) {
        val presentation: Presentation = e.presentation
        val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)

        val isVisible = virtualFile?.name?.endsWith(".apk") ?: false
        presentation.isEnabledAndVisible = isVisible
    }
}