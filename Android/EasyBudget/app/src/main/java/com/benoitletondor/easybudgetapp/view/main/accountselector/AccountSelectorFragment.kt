package com.benoitletondor.easybudgetapp.view.main.accountselector

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import com.benoitletondor.easybudgetapp.databinding.FragmentAccountSelectorBinding
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.viewLifecycleScope
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.main.accountselector.view.AccountsView
import com.benoitletondor.easybudgetapp.view.main.createaccount.CreateAccountActivity
import com.benoitletondor.easybudgetapp.view.main.login.LoginActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountSelectorFragment : BottomSheetDialogFragment() {
    private var binding: FragmentAccountSelectorBinding? = null

    private val viewModel: AccountSelectorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAccountSelectorBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.root?.setContent {
            AppTheme {
                AccountsView(viewModel)
            }
        }

        viewLifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                is AccountSelectorViewModel.Event.AccountSelected -> {
                    (activity as? MainActivity)?.onAccountSelectedFromBottomSheet(event.account)
                    dismiss()
                }
                AccountSelectorViewModel.Event.OpenProScreen -> {
                    activity?.let { activity ->
                        val startIntent = Intent(activity, SettingsActivity::class.java)
                        startIntent.putExtra(SettingsActivity.SHOW_PRO_INTENT_KEY, true)
                        ActivityCompat.startActivityForResult(activity, startIntent, MainActivity.SETTINGS_SCREEN_ACTIVITY_CODE, null)
                    }
                    dismiss()
                }
                AccountSelectorViewModel.Event.OpenLoginScreen -> {
                    activity?.let {
                        it.startActivity(Intent(it, LoginActivity::class.java))
                    }
                }
                AccountSelectorViewModel.Event.OpenCreateAccountScreen -> {
                    activity?.let {
                        it.startActivity(Intent(it, CreateAccountActivity::class.java))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}