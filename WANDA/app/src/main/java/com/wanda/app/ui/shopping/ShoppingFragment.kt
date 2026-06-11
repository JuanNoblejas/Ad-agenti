package com.wanda.app.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.wanda.app.databinding.DialogAddItemBinding
import com.wanda.app.databinding.FragmentShoppingBinding

class ShoppingFragment : Fragment() {

    private var _binding: FragmentShoppingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingViewModel by viewModels()
    private lateinit var adapter: ShoppingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingAdapter(
            onToggleChecked = { item ->
                viewModel.toggleChecked(item)
            },
            onDelete = { item ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete item")
                    .setMessage("Are you sure you want to delete \"${item.name}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteItem(item)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvShopping.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ShoppingFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            if (items.isEmpty()) {
                binding.tvEmptyList.visibility = View.VISIBLE
                binding.rvShopping.visibility = View.GONE
            } else {
                binding.tvEmptyList.visibility = View.GONE
                binding.rvShopping.visibility = View.VISIBLE
            }

            // Show/hide delete checked button based on whether any items are checked
            val hasCheckedItems = items.any { it.isChecked }
            binding.btnDeleteChecked.visibility = if (hasCheckedItems) View.VISIBLE else View.GONE
        }
    }

    private fun setupButtons() {
        binding.fabAdd.setOnClickListener {
            showAddItemDialog()
        }

        binding.btnDeleteChecked.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete completed")
                .setMessage("Delete all items marked as purchased?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCheckedItems()
                    Toast.makeText(requireContext(), "Items deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAddItemDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAdd.setOnClickListener {
            val itemName = dialogBinding.etItemName.text.toString().trim()

            if (itemName.isEmpty()) {
                dialogBinding.etItemName.error = "Enter the item name"
                return@setOnClickListener
            }

            viewModel.addItem(itemName)
            Toast.makeText(requireContext(), "\"$itemName\" added", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()

        // Request focus and show keyboard
        dialogBinding.etItemName.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
