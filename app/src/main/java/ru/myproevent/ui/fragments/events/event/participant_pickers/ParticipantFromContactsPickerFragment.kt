package ru.myproevent.ui.fragments.events.event.participant_pickers

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import moxy.ktx.moxyPresenter
import ru.myproevent.ProEventApp
import ru.myproevent.R
import ru.myproevent.databinding.FragmentParticipantPickerFromContactsBinding
import ru.myproevent.domain.models.entities.Contact
import ru.myproevent.ui.fragments.BaseMvpFragment
import ru.myproevent.ui.presenters.events.event.participant_pickers.participant_from_contacts_picker.ParticipantFromContactsPickerPresenter
import ru.myproevent.ui.presenters.events.event.participant_pickers.participant_from_contacts_picker.ParticipantFromContactsPickerView
import ru.myproevent.ui.presenters.events.event.participant_pickers.participant_from_contacts_picker.adapters.ContactsPickerRVAdapter
import ru.myproevent.ui.presenters.events.event.participant_pickers.participant_from_contacts_picker.adapters.PickedContactsRVAdapter
import ru.myproevent.ui.presenters.main.BottomNavigation
import ru.myproevent.ui.presenters.main.RouterProvider


// TODO: отрефаторить: этот фрагмент во многом копирует ContactsFragment
class ParticipantFromContactsPickerFragment :
    BaseMvpFragment<FragmentParticipantPickerFromContactsBinding>(
        FragmentParticipantPickerFromContactsBinding::inflate
    ), ParticipantFromContactsPickerView {

    companion object {
        const val PARTICIPANTS_IDS_ARG = "PARTICIPANTS_IDS"
        fun newInstance(participantsIds: List<Long>) =
            ParticipantFromContactsPickerFragment().apply {
                arguments = Bundle().apply {
                    putLongArray(
                        PARTICIPANTS_IDS_ARG,
                        participantsIds.toLongArray()
                    )
                }
            }
    }

    private var isFilterOptionsExpanded = false

    // TODO: копирует поле licenceTouchListener из RegistrationFragment
    private val filterOptionTouchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> with(v as TextView) {
                setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_600))
                setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_white))
            }
            MotionEvent.ACTION_UP -> with(v as TextView) {
                setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_white))
                setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_800))
                performClick()
            }
        }
        true
    }

    private fun selectFilterOption(option: TextView) {
        with(binding) {
            allContacts.setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_white))
            allContacts.setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_800))
            outgoingContacts.setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_white))
            outgoingContacts.setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_800))
            incomingContacts.setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_white))
            incomingContacts.setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_800))

            option.setBackgroundColor(ProEventApp.instance.getColor(R.color.ProEvent_blue_600))
            option.setTextColor(ProEventApp.instance.getColor(R.color.ProEvent_white))

            when (option.id) {
                R.id.all_contacts -> {
                    // TODO: Вынести в ресурсы
                    title.text = getString(R.string.contacts_all)
                    noContactsText.text = getString(R.string.you_have_no_contacts)
                }
                R.id.outgoing_contacts -> {
                    title.text = getString(R.string.contacts_outgoing)
                    noContactsText.text = getString(R.string.you_have_no_active_requests)
                }

                R.id.incoming_contacts -> {
                    title.text = getString(R.string.contacts_incoming)
                    noContactsText.text = getString(R.string.you_have_no_active_requests)
                }
            }
        }
    }

    override val presenter by moxyPresenter {
        ParticipantFromContactsPickerPresenter(
            localRouter = (parentFragment as RouterProvider).router,
            eventParticipantsIds = requireArguments().getLongArray(
                ParticipantPickerTypeSelectionFragment.PARTICIPANTS_IDS_ARG
            )!!.toList()
        ).apply {
            ProEventApp.instance.appComponent.inject(this)
        }
    }

    var contactsAdapter: ContactsPickerRVAdapter? = null

    var pickedContactsAdapter: PickedContactsRVAdapter? = null

    private var confirmScreenCallBack: ((confirmed: Boolean) -> Unit)? = null

    private fun showFilterOptions() {
        isFilterOptionsExpanded = true
        with(binding) {
            filter.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.ProEvent_bright_orange_300
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            searchEdit.hideKeyBoard() // TODO: нужно вынести это в вызов предществующий данному, чтобы тень при скрытии клавиатуры отображалась корректно
            searchInput.visibility = View.GONE
            rvPickedContacts.visibility = View.GONE
            if (toShowPickedParticipants) {
                pickedContactsCount.isVisible = false
                separator.isVisible = false
            }
            shadow.visibility = View.VISIBLE
            allContacts.visibility = View.VISIBLE
            outgoingContacts.visibility = View.VISIBLE
            incomingContacts.visibility = View.VISIBLE
        }
    }

    private fun hideFilterOptions() {
        isFilterOptionsExpanded = false
        with(binding) {
            filter.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.ProEvent_blue_800
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            searchInput.visibility = View.VISIBLE
            rvPickedContacts.visibility = View.VISIBLE
            if (toShowPickedParticipants) {
                pickedContactsCount.isVisible = true
                separator.isVisible = true
            }
            shadow.visibility = View.GONE
            allContacts.visibility = View.GONE
            outgoingContacts.visibility = View.GONE
            incomingContacts.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        // BottomNavigation скрывается, чтобы пользователь не мог перейти на вкладку контактов и изменить свой спискок контактов,
        // так как это привод к тому что список контактов пользователя в открытом ParticipantFromContactsPickerFragment будет отличаться,
        // так как он не обновляется
        (requireActivity() as BottomNavigation).hideBottomNavigation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            allContacts.setOnTouchListener(filterOptionTouchListener)
            allContacts.setOnClickListener {
                selectFilterOption(allContacts)
                presenter.loadData(Contact.Status.ALL)
                hideFilterOptions()
            }
            outgoingContacts.setOnTouchListener(filterOptionTouchListener)
            outgoingContacts.setOnClickListener {
                selectFilterOption(outgoingContacts)
                presenter.loadData(Contact.Status.PENDING)
                hideFilterOptions()
            }
            incomingContacts.setOnTouchListener(filterOptionTouchListener)
            incomingContacts.setOnClickListener {
                selectFilterOption(incomingContacts)
                presenter.loadData(Contact.Status.REQUESTED)
                hideFilterOptions()
            }
            filter.setOnClickListener {
                if (!isFilterOptionsExpanded) {
                    showFilterOptions()
                } else {
                    hideFilterOptions()
                }
            }
            filterHitArea.setOnClickListener { filter.performClick() }
            shadow.setOnClickListener { hideFilterOptions() }
            btnYes.setOnClickListener { confirmScreenCallBack?.invoke(true) }
            btnNo.setOnClickListener { confirmScreenCallBack?.invoke(false) }
            confirm.setOnClickListener {
                Log.d("[MYLOG]", "confirm.setOnClickListener")
                presenter.confirmPick()
            }

            // https://stackoverflow.com/questions/20103888/animatelayoutchanges-does-not-work-well-with-nested-layout
            container.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.init()
    }

    fun initRvContacts() = with(binding) {
        rvContacts.layoutManager = LinearLayoutManager(context)
        contactsAdapter = ContactsPickerRVAdapter(presenter.contactsPickerListPresenter)
        rvContacts.adapter = contactsAdapter
    }

    fun initRvPickedContacts() = with(binding) {
        rvPickedContacts.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        pickedContactsAdapter = PickedContactsRVAdapter(presenter.pickedContactsListPresenter)
        rvPickedContacts.adapter = pickedContactsAdapter
    }

    override fun init() {
        initRvContacts()
        initRvPickedContacts()
    }

    override fun hideConfirmationScreen() {
        binding.container.visibility = View.VISIBLE
        binding.confirmScreen.visibility = View.GONE
    }

    override fun showConfirmationScreen(
        message: String,
        callBack: ((confirmed: Boolean) -> Unit)?
    ) {
        binding.tvConfirmMsg.text = message
        confirmScreenCallBack = callBack

        binding.container.visibility = View.INVISIBLE
        binding.confirmScreen.visibility = View.VISIBLE
    }

    override fun updateContactsList() {
        if (contactsAdapter != null) {
            contactsAdapter!!.notifyDataSetChanged()
            with(binding) {
                if (contactsAdapter!!.itemCount == 0) {
                    progressBar.visibility = View.GONE
                    scroll.visibility = View.VISIBLE
                    noContacts.visibility = View.VISIBLE
                    noContactsText.visibility = View.VISIBLE
                } else if (contactsAdapter!!.itemCount > 0) {
                    progressBar.visibility = View.GONE
                    noContacts.visibility = View.GONE
                    noContactsText.visibility = View.GONE
                    scroll.visibility = View.VISIBLE
                    rvContacts.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun updatePickedContactsList() {
        pickedContactsAdapter?.notifyDataSetChanged()
    }

    private var toShowPickedParticipants = false

    override fun showPickedParticipants() = with(binding) {
        if (pickedContactsCount.isVisible) {
            return
        }
        toShowPickedParticipants = true
        pickedContactsCount.isVisible = true
        separator.isVisible = true
    }

    override fun hidePickedParticipants() = with(binding) {
        if (!pickedContactsCount.isVisible) {
            return
        }
        toShowPickedParticipants = false
        pickedContactsCount.isVisible = false
        separator.isVisible = false
    }

    override fun setPickedParticipantsCount(curr: Int, all: Int) {
        binding.pickedContactsCount.text =
            getString(R.string.picked_contacts_count_format, curr, all)
    }

    override fun showToast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    override fun setResult(requestKey: String, result: Bundle) {
        parentFragmentManager.setFragmentResult(requestKey, result)
    }

    override fun setProgressBarVisibility(visible: Boolean) = Unit
    override fun hideNoContactsLayout() = Unit
    override fun showNoContactsLayout(status: Contact.Status) = Unit

    override fun onStop() {
        super.onStop()
        (requireActivity() as BottomNavigation).showBottomNavigation()
    }

    override fun onBackPressed() = presenter.onBackPressed()
}