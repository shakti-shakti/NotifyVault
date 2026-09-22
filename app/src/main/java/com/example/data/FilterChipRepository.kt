package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CustomFilterChip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val packages: Set<String> = emptySet(),
    val keywords: List<String> = emptyList(),
    val regex: String? = null,
    val colorHex: String = "#E2A84B",
    val iconName: String = "Filter",
    val orderIndex: Int = 0
)

class FilterChipRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notify_vault_chips_repo", Context.MODE_PRIVATE)

    // Hidden built-in chips (e.g. "System")
    private val _hiddenBuiltInChips = MutableStateFlow(loadHiddenBuiltIns())
    val hiddenBuiltInChips: StateFlow<Set<String>> = _hiddenBuiltInChips.asStateFlow()

    // Custom user filter chips
    private val _customChips = MutableStateFlow(loadCustomChips())
    val customChips: StateFlow<List<CustomFilterChip>> = _customChips.asStateFlow()

    // Dashboard Quick Chips (ordered list of package names)
    private val _quickChipPackages = MutableStateFlow(loadQuickChips())
    val quickChipPackages: StateFlow<List<String>> = _quickChipPackages.asStateFlow()

    // Temporary active quick chip (single app filter from the app grid button)
    private val _temporaryQuickChip = MutableStateFlow<String?>(null)
    val temporaryQuickChip: StateFlow<String?> = _temporaryQuickChip.asStateFlow()

    // Has user completed initial quick-chip selection setup?
    private val _hasPromptedQuickChipsSetup = MutableStateFlow(prefs.getBoolean(KEY_QUICK_SETUP_DONE, false))
    val hasPromptedQuickChipsSetup: StateFlow<Boolean> = _hasPromptedQuickChipsSetup.asStateFlow()

    fun markQuickChipsSetupCompleted() {
        prefs.edit().putBoolean(KEY_QUICK_SETUP_DONE, true).apply()
        _hasPromptedQuickChipsSetup.value = true
    }

    // --- CUSTOM FILTER CHIPS MANAGEMENT ---
    fun addCustomChip(name: String, packages: Set<String>, colorHex: String) {
        val current = _customChips.value.toMutableList()
        val newChip = CustomFilterChip(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            packages = packages,
            colorHex = colorHex,
            orderIndex = current.size
        )
        current.add(newChip)
        saveCustomChips(current)
    }

    fun updateCustomChip(chip: CustomFilterChip) {
        val current = _customChips.value.map { if (it.id == chip.id) chip else it }
        saveCustomChips(current)
    }

    fun saveCustomChip(chip: CustomFilterChip) {
        val current = _customChips.value.toMutableList()
        val index = current.indexOfFirst { it.id == chip.id }
        if (index >= 0) {
            current[index] = chip
        } else {
            current.add(chip)
        }
        saveCustomChips(current)
    }

    fun renameCustomChip(id: String, newName: String) {
        val current = _customChips.value.map { if (it.id == id) it.copy(name = newName) else it }
        saveCustomChips(current)
    }

    fun changeCustomChipColor(id: String, newColorHex: String) {
        val current = _customChips.value.map { if (it.id == id) it.copy(colorHex = newColorHex) else it }
        saveCustomChips(current)
    }

    fun duplicateCustomChip(id: String) {
        val chip = _customChips.value.find { it.id == id } ?: return
        val current = _customChips.value.toMutableList()
        val copy = chip.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${chip.name} (Copy)".take(20),
            orderIndex = current.size
        )
        current.add(copy)
        saveCustomChips(current)
    }

    fun deleteCustomChip(id: String) {
        val current = _customChips.value.filter { it.id != id }
        saveCustomChips(current)
    }

    fun hideBuiltInChip(name: String) {
        val current = _hiddenBuiltInChips.value.toMutableSet()
        current.add(name)
        saveHiddenBuiltIns(current)
    }

    fun unhideBuiltInChip(name: String) {
        val current = _hiddenBuiltInChips.value.toMutableSet()
        current.remove(name)
        saveHiddenBuiltIns(current)
    }

    private fun saveHiddenBuiltIns(set: Set<String>) {
        val arr = JSONArray(set)
        prefs.edit().putString(KEY_HIDDEN_BUILT_INS, arr.toString()).apply()
        _hiddenBuiltInChips.value = set
    }

    private fun loadHiddenBuiltIns(): Set<String> {
        val json = prefs.getString(KEY_HIDDEN_BUILT_INS, null) ?: return emptySet()
        return try {
            val arr = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) set.add(arr.getString(i))
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveCustomChips(chips: List<CustomFilterChip>) {
        val arr = JSONArray()
        chips.forEach { chip ->
            val obj = JSONObject().apply {
                put("id", chip.id)
                put("name", chip.name)
                put("packages", JSONArray(chip.packages))
                put("keywords", JSONArray(chip.keywords))
                put("regex", chip.regex ?: "")
                put("colorHex", chip.colorHex)
                put("iconName", chip.iconName)
                put("orderIndex", chip.orderIndex)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_CHIPS, arr.toString()).apply()
        _customChips.value = chips
    }

    private fun loadCustomChips(): List<CustomFilterChip> {
        val json = prefs.getString(KEY_CUSTOM_CHIPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<CustomFilterChip>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pkgArr = obj.getJSONArray("packages")
                val pkgSet = mutableSetOf<String>()
                for (j in 0 until pkgArr.length()) {
                    pkgSet.add(pkgArr.getString(j))
                }
                val kwArr = obj.optJSONArray("keywords") ?: JSONArray()
                val kwList = mutableListOf<String>()
                for (k in 0 until kwArr.length()) {
                    kwList.add(kwArr.getString(k))
                }
                val regexVal = obj.optString("regex", "").ifBlank { null }
                list.add(
                    CustomFilterChip(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        packages = pkgSet,
                        keywords = kwList,
                        regex = regexVal,
                        colorHex = obj.optString("colorHex", "#E2A84B"),
                        iconName = obj.optString("iconName", "Filter"),
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }
            list.sortedBy { it.orderIndex }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- QUICK CHIPS MANAGEMENT ---
    fun setQuickChips(packages: List<String>) {
        val limited = packages.distinct().take(6)
        val arr = JSONArray(limited)
        prefs.edit().putString(KEY_QUICK_CHIPS, arr.toString()).apply()
        _quickChipPackages.value = limited
    }

    fun addQuickChip(pkg: String) {
        val current = _quickChipPackages.value.toMutableList()
        if (!current.contains(pkg)) {
            if (current.size < 6) {
                current.add(pkg)
                setQuickChips(current)
            }
        }
    }

    fun removeQuickChip(pkg: String) {
        val current = _quickChipPackages.value.filter { it != pkg }
        setQuickChips(current)
        if (_temporaryQuickChip.value == pkg) {
            _temporaryQuickChip.value = null
        }
    }

    fun setTemporaryQuickChip(pkg: String?) {
        _temporaryQuickChip.value = pkg
    }

    fun pinTemporaryQuickChip(pkg: String) {
        addQuickChip(pkg)
        if (_temporaryQuickChip.value == pkg) {
            _temporaryQuickChip.value = null
        }
    }

    private fun loadQuickChips(): List<String> {
        val json = prefs.getString(KEY_QUICK_CHIPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list.distinct().take(6)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_CUSTOM_CHIPS = "key_custom_chips"
        private const val KEY_QUICK_CHIPS = "key_quick_chips"
        private const val KEY_QUICK_SETUP_DONE = "key_quick_setup_done"
        private const val KEY_HIDDEN_BUILT_INS = "key_hidden_built_ins"

        @Volatile
        private var instance: FilterChipRepository? = null

        fun getInstance(context: Context): FilterChipRepository {
            return instance ?: synchronized(this) {
                instance ?: FilterChipRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
