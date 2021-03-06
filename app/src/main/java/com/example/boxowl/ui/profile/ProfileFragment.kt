package com.example.boxowl.ui.profile

import android.app.Activity.MODE_PRIVATE
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.boxowl.AuthActivity
import com.example.boxowl.R
import com.example.boxowl.bases.FragmentInteractionListener
import com.example.boxowl.databinding.FragmentProfileBinding
import com.example.boxowl.models.Courier
import com.example.boxowl.models.CurrentCourier
import com.example.boxowl.presentation.profile.ProfileContract
import com.example.boxowl.presentation.profile.ProfilePresenter
import com.example.boxowl.ui.extension.onClick
import com.example.boxowl.utils.*
import com.google.gson.Gson
import com.wada811.viewbinding.viewBinding
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.watchers.FormatWatcher
import ru.tinkoff.decoro.watchers.MaskFormatWatcher
import java.io.ByteArrayOutputStream


/**
 * Created by Andrey Morgunov on 27/10/2020.
 */

class ProfileFragment : Fragment(R.layout.fragment_profile), ProfileContract.View {

    interface OnProfileFragmentInteractionListener : FragmentInteractionListener

    private lateinit var listener: OnProfileFragmentInteractionListener

    private val binding: FragmentProfileBinding by viewBinding()
    private lateinit var profilePresenter: ProfilePresenter
    private lateinit var loadingDialog: AlertDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profilePresenter = ProfilePresenter(this)
        loadingDialog = loadingSpotsDialog(requireContext())

        profilePresenter.getCourier(CurrentCourier.courier.CourierId)

        with(binding) {
            changeUserDataBtn.onClick {
                showLoadingDialog(loadingDialog)
                profilePresenter.updateCourierData(changedUser())
            }
            changeUserImageBtn.onClick { getImageFromGallery() }
            logOutBtn.onClick { logOut() }
        }
        loadChangeProfileTextWatcher()

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(
                true
            ) {
                override fun handleOnBackPressed() {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            })
    }

    private fun logOut() {
        AlertDialog.Builder(activity)
            .setMessage(R.string.dialog_logout_message)
            .setTitle(R.string.dialog_confirmation_title)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val sharedPreferencesUser =
                    requireActivity().getSharedPreferences("COURIER", MODE_PRIVATE)
                sharedPreferencesUser.edit().remove("CourierObject").apply()
                sharedPreferencesUser.edit().remove("CourierPinCode").apply()
                startActivity(Intent(activity, AuthActivity::class.java))
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    private fun getImageFromGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            val inputStream = requireActivity().contentResolver.openInputStream(imageUri!!)
            val scaleBm =
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeStream(inputStream),
                    96,
                    96,
                    true
                )
            Glide.with(this)
                .load(scaleBm)
                .into(binding.userImageView)
            val outputStream = ByteArrayOutputStream()
            scaleBm.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            CurrentCourier.courier.CourierImage =
                Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }
    }

    private fun loadChangeProfileTextWatcher() {
        val changeProfileTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.changeUserDataBtn.isEnabled =
                    binding.firstNameEditText.text.toString().isNotBlank()
                            && binding.secondNameEditText.text.toString().isNotBlank()
                            && binding.phoneEditText.text.toString().isPhone()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        with(binding) {
            firstNameEditText.addTextChangedListener(changeProfileTextWatcher)
            secondNameEditText.addTextChangedListener(changeProfileTextWatcher)
            phoneEditText.addTextChangedListener(changeProfileTextWatcher)
        }
    }

    private fun changedUser(): Courier {
        val user = CurrentCourier.courier
        with(user) {
            CourierName = binding.firstNameEditText.text.toString()
            CourierSurname = binding.secondNameEditText.text.toString()
            CourierPhone = binding.phoneEditText.text.toString().toNormalString()
        }
        return user
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnProfileFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(requireContext().toString())
        }
    }

    override fun onStart() {
        super.onStart()
        listener.setBottomNavigation(true, R.id.navigation_profile)
        listener.setToolbarTitle(resources.getString(R.string.title_profile))
    }

    override fun onLoadSuccess(courier: Courier) {
        val mask = MaskImpl.createTerminated(PredefinedSlots.RUS_PHONE_NUMBER)
        val watcher: FormatWatcher = MaskFormatWatcher(mask)
        watcher.installOn(binding.phoneEditText)

        if (courier.CourierImage == null) {
            binding.userImageView.setImageResource(R.drawable.default_user_image)
        } else {
            Glide.with(this)
                .load(Base64.decode(courier.CourierImage, Base64.DEFAULT))
                .into(binding.userImageView)
        }
        binding.firstNameEditText.setText(courier.CourierName)
        binding.secondNameEditText.setText(courier.CourierSurname)
        binding.phoneEditText.setText(courier.CourierPhone)
        binding.ratingTextView.setText(courier.CourierRating.toString())
        binding.moneyTextView.setText(courier.CourierMoney.toString())

        saveCourierInSharedPrefs(courier)
    }

    override fun onError(error: String) {
        dismissLoadingDialog(loadingDialog)
        showToast(requireContext(), error)
    }

    override fun onUpdateSuccess() {
        dismissLoadingDialog(loadingDialog)
        saveCourierInSharedPrefs(changedUser())
        showToast(requireContext(), "Данные изменены")
    }

    private fun saveCourierInSharedPrefs(courier: Courier) {
        val sharedPref = requireActivity().getSharedPreferences("COURIER", MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(courier)
        with(sharedPref.edit()) {
            putString("CourierObject", json)
            apply()
        }
    }

    override fun onDestroy() {
        profilePresenter.onDestroy()
        super.onDestroy()
    }
}