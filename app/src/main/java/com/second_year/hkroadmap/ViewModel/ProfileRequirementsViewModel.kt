package com.second_year.hkroadmap.ViewModel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.data.models.ProfileRequirementsData
import com.second_year.hkroadmap.data.models.RequirementGroup
import com.second_year.hkroadmap.Utils.NetworkUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileRequirementsViewModel(private val apiService: ApiService) : ViewModel() {

    private val _uiState = MutableLiveData<ProfileRequirementsUiState>()
    val uiState: LiveData<ProfileRequirementsUiState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _exportFileUri = MutableLiveData<Uri?>()
    val exportFileUri: LiveData<Uri?> = _exportFileUri

    fun fetchProfileRequirements(token: String, context: Context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            _uiState.value = ProfileRequirementsUiState.Error("No internet connection. Please check your network and try again.")
            return
        }

        _uiState.value = ProfileRequirementsUiState.Loading

        viewModelScope.launch {
            try {
                val response = apiService.getProfileRequirements("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.let { profileRequirementsResponse ->
                        if (profileRequirementsResponse.success) {
                            val data = profileRequirementsResponse.data
                            _uiState.value = ProfileRequirementsUiState.Success(data)
                        } else {
                            _uiState.value = ProfileRequirementsUiState.Error("Failed to load profile requirements")
                        }
                    } ?: run {
                        _uiState.value = ProfileRequirementsUiState.Error("Empty response from server")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Unauthorized. Please log in again."
                        404 -> "Profile not found."
                        500 -> "Server error. Please try again later."
                        else -> "Error: ${response.message()}"
                    }
                    _uiState.value = ProfileRequirementsUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("ProfileReqViewModel", "Error fetching profile requirements", e)
                _uiState.value = ProfileRequirementsUiState.Error("Failed to connect to server: ${e.message}")
            }
        }
    }

    fun exportProfileRequirements(context: Context) {
        val currentState = _uiState.value
        if (currentState !is ProfileRequirementsUiState.Success) {
            _uiState.value = ProfileRequirementsUiState.Error("No data available to export")
            return
        }

        _isLoading.value = true // Set loading to true

        viewModelScope.launch {
            try {
                val data = currentState.data

                // Create PDF file
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "profile_requirements_$timeStamp.pdf"
                val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(storageDir, fileName)

                // Generate PDF
                generatePdf(context, file, data)

                // Get URI for the file using FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                _exportFileUri.value = uri

            } catch (e: Exception) {
                Log.e("ProfileReqViewModel", "Error exporting data", e)
                _uiState.value = ProfileRequirementsUiState.Error("Failed to export data: ${e.message}")
                _isLoading.value = false // Set loading to false on error
            }
        }
    }

    // Add method to reset loading state
    fun resetLoadingState() {
        _isLoading.value = false
    }

    private fun generatePdf(context: Context, file: File, data: ProfileRequirementsData) {
        // Create Document
        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        // Get colors from resources
        val primaryGreenColor = ContextCompat.getColor(context, R.color.primary_green)
        val primaryGreen = BaseColor(
            (primaryGreenColor shr 16) and 0xFF,
            (primaryGreenColor shr 8) and 0xFF,
            primaryGreenColor and 0xFF
        )
        val black = BaseColor(0, 0, 0)
        val gray = BaseColor(128, 128, 128)
        val lightGray = BaseColor(211, 211, 211)

        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, primaryGreen)
        val title = Paragraph("HAWAK KAMAY ROADMAP - PHINMA UNIVERSITY OF PANGASINAN", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Add profile section
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, primaryGreen)
        val profileSection = Paragraph("STUDENT INFORMATION", sectionFont)
        profileSection.spacingAfter = 10f
        document.add(profileSection)

        // Create profile table
        val profileTable = PdfPTable(2)
        profileTable.widthPercentage = 100f
        profileTable.setWidths(floatArrayOf(1f, 2f))

        // Add profile data
        addProfileRow(profileTable, "Name", data.profile.name ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Email", data.profile.email ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Student Number", data.profile.studentNumber ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Department", data.profile.department ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Program", data.profile.collegeProgram ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Year Level", data.profile.yearLevel?.toString() ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Scholarship Type", data.profile.scholarshipType ?: "Not set", black, gray, lightGray)
        addProfileRow(profileTable, "Contact Number", data.profile.contactNumber ?: "Not set", black, gray, lightGray)

        document.add(profileTable)
        document.add(Paragraph("\n"))

        // Add requirements section
        val requirementsSection = Paragraph("STUDENT INVOLVEMENTS", sectionFont)
        requirementsSection.spacingAfter = 10f
        document.add(requirementsSection)

        // Add requirements for each event
        data.requirements.forEach { (eventName, requirements) ->
            // Add event name
            val eventFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, black)
            val eventParagraph = Paragraph(eventName, eventFont)
            eventParagraph.spacingBefore = 15f
            eventParagraph.spacingAfter = 5f
            document.add(eventParagraph)

            // Create requirements table
            val reqTable = PdfPTable(3)
            reqTable.widthPercentage = 100f
            reqTable.setWidths(floatArrayOf(1.5f, 1f, 1f))

            // Add table headers
            addTableHeader(reqTable, "Requirement", "Submission Date", "Approved By", primaryGreen)

            // Add requirements
            requirements.forEach { requirement ->
                addRequirementRow(
                    reqTable,
                    requirement.requirement,
                    requirement.submissionDate?.let { formatDate(it) } ?: "N/A",
                    requirement.approvedBy ?: "N/A",
                    black, gray
                )
            }

            document.add(reqTable)
        }

        // Add remarks section
        document.add(Paragraph("\n\n"))
        val remarksFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, black)
        val remarksParagraph = Paragraph("REMARKS FOR HK ENDORSEMENT", remarksFont)
        remarksParagraph.spacingAfter = 10f
        document.add(remarksParagraph)

        // Add a remarks box
        val remarksCell = PdfPCell()
        remarksCell.fixedHeight = 80f // Height for remarks
        remarksCell.border = Rectangle.BOX
        remarksCell.borderColor = lightGray

        val remarksTable = PdfPTable(1)
        remarksTable.widthPercentage = 100f
        remarksTable.addCell(remarksCell)
        document.add(remarksTable)

        document.add(Paragraph("\n"))

        // Add signature line
        val signatureTable = PdfPTable(1)
        signatureTable.widthPercentage = 80f
        signatureTable.horizontalAlignment = Element.ALIGN_RIGHT

        // Signature line
        val sigCell1 = PdfPCell(Paragraph("_______________________________"))
        sigCell1.horizontalAlignment = Element.ALIGN_CENTER
        sigCell1.border = Rectangle.NO_BORDER
        signatureTable.addCell(sigCell1)

        // Label
        val sigCell2 = PdfPCell(Paragraph("Signed by: (signature over printed name)"))
        sigCell2.horizontalAlignment = Element.ALIGN_CENTER
        sigCell2.border = Rectangle.NO_BORDER
        signatureTable.addCell(sigCell2)

        // Date
        val sigCell3 = PdfPCell(Paragraph("(mm/dd/yy)"))
        sigCell3.horizontalAlignment = Element.ALIGN_CENTER
        sigCell3.border = Rectangle.NO_BORDER
        signatureTable.addCell(sigCell3)

        document.add(signatureTable)

        document.close()
    }

    private fun addProfileRow(table: PdfPTable, label: String, value: String, labelColor: BaseColor, valueColor: BaseColor, borderColor: BaseColor) {
        val labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, labelColor)
        val valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, valueColor)

        val labelCell = PdfPCell(Paragraph(label, labelFont))
        labelCell.paddingBottom = 5f
        labelCell.paddingTop = 5f
        labelCell.border = Rectangle.BOTTOM
        labelCell.borderColor = borderColor

        val valueCell = PdfPCell(Paragraph(value, valueFont))
        valueCell.paddingBottom = 5f
        valueCell.paddingTop = 5f
        valueCell.border = Rectangle.BOTTOM
        valueCell.borderColor = borderColor

        table.addCell(labelCell)
        table.addCell(valueCell)
    }

    private fun addTableHeader(table: PdfPTable, col1: String, col2: String, col3: String, headerColor: BaseColor) {
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, BaseColor.WHITE)

        val cell1 = PdfPCell(Paragraph(col1, headerFont))
        cell1.backgroundColor = headerColor
        cell1.paddingBottom = 5f
        cell1.paddingTop = 5f

        val cell2 = PdfPCell(Paragraph(col2, headerFont))
        cell2.backgroundColor = headerColor
        cell2.paddingBottom = 5f
        cell2.paddingTop = 5f

        val cell3 = PdfPCell(Paragraph(col3, headerFont))
        cell3.backgroundColor = headerColor
        cell3.paddingBottom = 5f
        cell3.paddingTop = 5f

        table.addCell(cell1)
        table.addCell(cell2)
        table.addCell(cell3)
    }

    private fun addRequirementRow(table: PdfPTable, requirement: String, submission: String, approval: String, textColor: BaseColor, altColor: BaseColor) {
        val font = FontFactory.getFont(FontFactory.HELVETICA, 10f, textColor)

        val cell1 = PdfPCell(Paragraph(requirement, font))
        cell1.paddingBottom = 5f
        cell1.paddingTop = 5f

        val cell2 = PdfPCell(Paragraph(submission, font))
        cell2.paddingBottom = 5f
        cell2.paddingTop = 5f

        val cell3 = PdfPCell(Paragraph(approval, font))
        cell3.paddingBottom = 5f
        cell3.paddingTop = 5f

        table.addCell(cell1)
        table.addCell(cell2)
        table.addCell(cell3)
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Assuming format is "yyyy-MM-dd HH:mm:ss"
            val parts = dateString.split(" ")
            if (parts.size >= 1) parts[0] else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    // Helper method to convert the map to a list of RequirementGroup for RecyclerView
    fun getRequirementGroups(data: ProfileRequirementsData): List<RequirementGroup> {
        return data.requirements.map { (eventName, requirements) ->
            RequirementGroup(eventName, requirements)
        }.sortedBy { it.eventName }
    }
}

// UI state sealed class for managing different states
sealed class ProfileRequirementsUiState {
    object Loading : ProfileRequirementsUiState()
    data class Success(val data: ProfileRequirementsData) : ProfileRequirementsUiState()
    data class Error(val message: String) : ProfileRequirementsUiState()
}