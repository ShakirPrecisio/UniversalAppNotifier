package com.example.universalappnotifier.ui.emailIdList

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.adapters.AddedEmailIdAdapter
import com.example.universalappnotifier.databinding.ActivityEmailIdListBinding
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.ui.addEmailId.AddEmailIdActivity
import com.example.universalappnotifier.utils.Utils

class EmailIdListActivity : AppCompatActivity(), AddedEmailIdAdapter.EmailRemovedListener {

    private var isEmailIdListUpdated = false
    private lateinit var emailIdList: ArrayList<CalendarEmailData>
    private lateinit var binding: ActivityEmailIdListBinding

    private lateinit var emailIdListViewModel: EmailIdListViewModel

    private val addEmailIdResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("is_email_id_added", false) != null) {
                isEmailIdListUpdated = data.getBooleanExtra("is_email_id_added", false)!!
            }
            Utils.printDebugLog("EmailIdListActivity: isNewEmailIdAdded: $isEmailIdListUpdated")
            if (isEmailIdListUpdated) {
                emailIdListViewModel.getUserAddedEmailIds(
                    giveGoogleEmailIds = true,
                    giveOutlookEmailIds = true
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailIdListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as MyApplication).appRepository
        emailIdListViewModel = ViewModelProvider(
            this,
            EmailIdListViewModelFactory(repository)
        )[EmailIdListViewModel::class.java]

        emailIdListViewModel.getUserAddedEmailIds(
            giveGoogleEmailIds = true,
            giveOutlookEmailIds = true
        )

        binding.fabAddEmailId.setOnClickListener {
            addEmailIdResultLauncher.launch(Intent(this, AddEmailIdActivity::class.java))
        }

        attachObservers()

    }

    private fun attachObservers() {
        emailIdListViewModel.userEmailIdListLiveData.observe(this@EmailIdListActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    isEmailIdListUpdated = true
                    if (it.data != null) {
                        emailIdList = it.data
                    }
                    val adapter = AddedEmailIdAdapter(emailIdList, this@EmailIdListActivity, this@EmailIdListActivity)
                    binding.rvAddedEmailIds.adapter = adapter
                    if (emailIdList.isEmpty()) {
                        Utils.showShortToast(this@EmailIdListActivity, "No Email Id Added!")
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@EmailIdListActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }
    }

    override fun onEmailIdRemoved(position: Int, itemData: CalendarEmailData) {
        emailIdListViewModel.removeEmailId(itemData, itemData.email_type)
    }

    override fun onBackPressed() {
        val intent = Intent().apply {
            putExtra("is_email_list_updated", isEmailIdListUpdated) // Set the boolean result accordingly
        }
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
    }


}