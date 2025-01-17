package ru.myproevent.domain.models.repositories.internet_access_info

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.myproevent.R
import ru.myproevent.domain.models.repositories.resourceProvider.IResourceProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class InternetAccessInfoRepository @Inject constructor(private val resourceProvider: IResourceProvider) :
    IInternetAccessInfoRepository {
    override fun hasInternetConnection(): Single<Boolean> {
        return Single.fromCallable {
            try {
                // Connect to Google DNS to check for connection
                val timeoutMs = 1500
                val socket = Socket()
                val socketAddress = InetSocketAddress(
                    resourceProvider.getString(R.string.check_internet_enabled_ip),
                    53
                )

                socket.connect(socketAddress, timeoutMs)
                socket.close()

                true
            } catch (e: IOException) {
                false
            }
        }
            .subscribeOn(Schedulers.io())
            // TODO: Вынести в Dagger
            .observeOn(AndroidSchedulers.mainThread())
    }
}