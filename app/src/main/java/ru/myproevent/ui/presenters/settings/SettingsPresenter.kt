package ru.myproevent.ui.presenters.settings

import ru.myproevent.domain.model.repositories.proevent_login.IProEventLoginRepository
import ru.myproevent.ui.presenters.BaseMvpPresenter
import javax.inject.Inject

class SettingsPresenter : BaseMvpPresenter<SettingsView>() {
    @Inject
    lateinit var loginRepository: IProEventLoginRepository

    fun account() {
        router.navigateTo(screens.account())
    }

    fun security() {
        router.navigateTo(screens.security())
    }

    fun logout() {
        loginRepository.logoutFromThisDevice()
        router.newRootScreen(screens.authorization())
    }
}