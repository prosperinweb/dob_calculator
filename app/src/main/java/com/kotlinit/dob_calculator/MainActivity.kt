package com.kotlinit.dob_calculator

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.kotlinit.dob_calculator.databinding.ActivityMainBinding
import java.time.Duration
import java.time.Period
import java.time.ZoneId
import java.util.Calendar

fun Animation.setAnimationEndListener(callback: () -> Unit) {
    this.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}

        override fun onAnimationEnd(animation: Animation?) {
            callback()
        }

        override fun onAnimationStart(animation: Animation?) {}
    })
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var btnCalculate: Button
    private lateinit var btnSelectDate: ImageView
    private lateinit var editTextDate: EditText
    private lateinit var resultViews: Map<TextView, Int>
    private lateinit var selectedDateView: TextView
    private var selectedBirthDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    companion object {
        const val DEFAULT_TEXT = "---"
        const val DATE_REGEX = "\\d{2}/\\d{2}/\\d{4}"
    }

    private fun init() {
        resultViews = mapOf(
            binding.textViewAgeInYears to R.string.label_years,
            binding.textViewAgeInMonths to R.string.label_months,
            binding.textViewAgeInWeeks to R.string.label_weeks,
            binding.textViewAgeInDays to R.string.label_days,
            binding.textViewAgeInHours to R.string.label_hours,
            binding.textViewAgeInMinutes to R.string.label_minutes,
            binding.textViewAgeInSeconds to R.string.label_seconds
        )
        btnCalculate = binding.btnCalculate
        btnSelectDate = binding.btnSelectDate
        editTextDate = binding.editTextDate
        selectedDateView = binding.selectedBirthDate
        setupListeners()
        resetUIFields()
    }

    private fun resetUIFields() {
        resetAgeTextFields()
        resetSelectedDateField()
    }

    private fun setupListeners() {
        btnCalculate.setOnClickListener {
            validateAndProcessDate(editTextDate.text.toString())

            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

            fadeOut.setAnimationEndListener {
                calculateAge()
                startAnimation(fadeIn)
            }
            startAnimation(fadeOut)
        }

        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        editTextDate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateAndProcessDate(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })

    }

    private fun validateAndProcessDate(dateString: String) {
        if (dateString.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show()
            btnCalculate.isEnabled = false
            return
        }

        if (dateString.matches(Regex(DATE_REGEX))) {
            // It matches the format dd/mm/yyyy
            val parts = dateString.split("/")

            try {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1  // months are 0-indexed
                val year = parts[2].toInt()

                val date = Calendar.getInstance().apply {
                    set(year, month, day)
                }

                if (date.timeInMillis > Calendar.getInstance().timeInMillis) {
                    // The date is in the future
                    Toast.makeText(this, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                    btnCalculate.isEnabled = false
                } else {
                    selectedBirthDate = date
                    btnCalculate.isEnabled = true
                }

            } catch (e: Exception) {
                // Something went wrong in parsing the date, you can show a toast or some feedback
                Toast.makeText(this, "Error processing date", Toast.LENGTH_SHORT).show()
                btnCalculate.isEnabled = false
            }
        } else {
            btnCalculate.isEnabled = false
        }
    }

    private fun resetAgeTextFields() {
        resultViews.forEach { (view, label) ->
            view.text = getString(label, DEFAULT_TEXT)
        }
    }

    private fun resetSelectedDateField() {
        selectedDateView.text = getString(R.string.label_selected_date, DEFAULT_TEXT)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        // Getting previously entered date from EditText
        val prevDate = binding.editTextDate.text.toString()
        if (prevDate.isNotEmpty() && prevDate.matches(Regex(DATE_REGEX))) {
            val parts = prevDate.split("/")
            calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                binding.editTextDate.setText(
                    String.format(
                        "%02d/%02d/%04d",
                        dayOfMonth,
                        month + 1,
                        year
                    )
                )
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set the maximum date for the DatePickerDialog to the current date
        datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
        datePickerDialog.show()
    }


    private fun startAnimation(animation: Animation) {
        resultViews.keys.forEach {
            it.startAnimation(animation)
        }
    }

    private fun onDateSelected(date: Calendar) {
        selectedBirthDate = date
        btnCalculate.isEnabled = true
        resetAgeTextFields()

        // Only the date picker should update the selectedDateView immediately
        selectedDateView.text = getString(
            R.string.label_selected_date,
            String.format(
                "%02d/%02d/%04d",
                date.get(Calendar.DAY_OF_MONTH),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.YEAR)
            )
        )
    }

    private fun calculateAge() {

        val selectedDateText = selectedDateView.text.toString()
        if (selectedDateText == getString(R.string.label_selected_date, DEFAULT_TEXT) ||
            selectedDateText != binding.editTextDate.text.toString()
        ) {
            selectedDateView.text = getString(
                R.string.label_selected_date,
                binding.editTextDate.text.toString()
            )
        }

        selectedBirthDate?.let { birthDate ->
            val currentDate = Calendar.getInstance()

            if (birthDate.after(currentDate)) {
                Toast.makeText(this, "Birth date cannot be in the future!", Toast.LENGTH_LONG)
                    .show()
                return
            }

            // Convert Calendar to LocalDate
            val startDate = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val endDate = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            // Calculate years, months and days difference
            val period = Period.between(startDate, endDate)


            // Convert Calendar to LocalDateTime for accurate time calculations
            val startTime = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

            // Calculate the time difference
            val duration = Duration.between(startTime, endTime)

            // Calculate age in different units
            val totalYears = period.years
            val totalMonths = period.years * 12 + period.months
            val totalWeeks = period.toTotalMonths() * 4 + period.days / 7
            val totalDays = period.toTotalMonths() * 30 + period.days
            val totalHours = duration.toHours()
            val totalMinutes = duration.toMinutes()
            val totalSeconds = duration.seconds


            resultViews.forEach { (view, label) ->
                when (view) {
                    binding.textViewAgeInYears -> view.text =
                        getString(label, totalYears.toString())

                    binding.textViewAgeInMonths -> view.text =
                        getString(label, totalMonths.toString())

                    binding.textViewAgeInWeeks -> view.text =
                        getString(label, totalWeeks.toString())

                    binding.textViewAgeInDays -> view.text =
                        getString(label, totalDays.toString())

                    binding.textViewAgeInHours -> view.text =
                        getString(label, totalHours.toString())

                    binding.textViewAgeInMinutes -> view.text =
                        getString(label, totalMinutes.toString())

                    binding.textViewAgeInSeconds -> view.text =
                        getString(label, totalSeconds.toString())
                }
            }
        }
    }
}