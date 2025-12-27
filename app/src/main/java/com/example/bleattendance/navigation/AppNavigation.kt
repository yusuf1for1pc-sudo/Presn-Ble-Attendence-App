// In navigation/AppNavigation.kt

package com.example.bleattendance.navigation // ✅ PACKAGE NAME CORRECTED

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel // ✅ IMPORT ADDED
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bleattendance.ui.screens.AddClassScreen
import com.example.bleattendance.ui.screens.ClassDetailsScreen
import com.example.bleattendance.ui.screens.ClassSessionScreen
import com.example.bleattendance.ui.screens.SessionDetailsScreen
import com.example.bleattendance.ui.screens.LoginScreen
import com.example.bleattendance.ui.screens.RoleSelectionScreen
import com.example.bleattendance.ui.screens.SplashScreen
import com.example.bleattendance.ui.screens.EnhancedStudentDashboardScreen
import com.example.bleattendance.ui.screens.StudentRegistrationScreenNew
import com.example.bleattendance.ui.screens.TeacherDashboardScreen
import com.example.bleattendance.ui.screens.TeacherRegistrationScreen
import com.example.bleattendance.ui.screens.ViewAllClassesScreen
import com.example.bleattendance.ui.screens.SettingsScreen
import com.example.bleattendance.ui.screens.StudentSettingsScreen
import com.example.bleattendance.ui.screens.ClassOptionsScreen
import com.example.bleattendance.ui.screens.TeacherAssignmentDashboardNew
import com.example.bleattendance.ui.screens.StudentAssignmentDashboardNew
import com.example.bleattendance.ui.screens.TeacherAssignmentCreationNew
import com.example.bleattendance.ui.screens.StudentAssignmentViewNew
import com.example.bleattendance.ui.screens.TeacherSubmissionReviewNew
import com.example.bleattendance.ui.screens.AIChatbotScreen
import com.example.bleattendance.ui.screens.AITodoListScreen
import com.example.bleattendance.ui.screens.StudentScanningScreen

@Composable
fun AppNavHost(permissionsGranted: Boolean = false) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController = navController)
        }

        composable("role_selection") {
            // ✅ VIEWMODEL PROVIDED HERE
            RoleSelectionScreen(navController = navController, viewModel = viewModel())
        }

        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("teacher_registration") {
            TeacherRegistrationScreen(navController = navController)
        }

        composable("student_registration") {
            StudentRegistrationScreenNew(navController = navController)
        }

        composable("teacher_dashboard") {
            TeacherDashboardScreen(navController = navController)
        }

        composable("student_dashboard") {
            EnhancedStudentDashboardScreen(navController = navController)
        }

        composable("ai_chatbot") {
            AIChatbotScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("ai_todo_list") {
            AITodoListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("student_scanning") {
            StudentScanningScreen(navController = navController)
        }

        composable(
            "add_class/{teacherEmail}",
            arguments = listOf(navArgument("teacherEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("teacherEmail") ?: ""
            AddClassScreen(navController = navController, teacherEmail = email)
        }

        composable(
            "class_options/{classId}/{subjectName}/{teacherName}/{groupId}/{teacherEmail}",
            arguments = listOf(
                navArgument("classId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType },
                navArgument("teacherName") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType },
                navArgument("teacherEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: 0
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: "Unknown Subject"
            val teacherName = backStackEntry.arguments?.getString("teacherName") ?: "Unknown Teacher"
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "Unknown Group"
            val teacherEmail = backStackEntry.arguments?.getString("teacherEmail") ?: ""
            ClassOptionsScreen(
                navController = navController,
                classId = classId,
                subjectName = subjectName,
                teacherName = teacherName,
                groupId = groupId,
                teacherEmail = teacherEmail
            )
        }

        composable(
            "class_details/{classId}/{subjectName}/{teacherName}/{groupId}",
            arguments = listOf(
                navArgument("classId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType },
                navArgument("teacherName") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: 0
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: "Unknown Subject"
            val teacherName = backStackEntry.arguments?.getString("teacherName") ?: "Unknown Teacher"
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "Unknown Group"
            ClassDetailsScreen(
                navController = navController, 
                classId = classId,
                subjectName = subjectName,
                teacherName = teacherName,
                groupId = groupId
            )
        }

        composable(
            "session_details/{sessionDate}/{sessionTime}/{sessionCode}/{sessionStatus}",
            arguments = listOf(
                navArgument("sessionDate") { type = NavType.StringType },
                navArgument("sessionTime") { type = NavType.StringType },
                navArgument("sessionCode") { type = NavType.StringType },
                navArgument("sessionStatus") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionDate = backStackEntry.arguments?.getString("sessionDate") ?: "Unknown Date"
            val sessionTime = backStackEntry.arguments?.getString("sessionTime") ?: "Unknown Time"
            val sessionCode = backStackEntry.arguments?.getString("sessionCode") ?: "Unknown Code"
            val sessionStatus = backStackEntry.arguments?.getString("sessionStatus") ?: "Unknown Status"
            
            // For now, we'll pass empty list - in a real implementation, you'd fetch this data
            SessionDetailsScreen(
                navController = navController,
                sessionDate = sessionDate,
                sessionTime = sessionTime,
                sessionCode = sessionCode,
                sessionStatus = sessionStatus,
                attendedStudents = emptyList() // TODO: Fetch actual student data
            )
        }

        // Student Assignment Dashboard (General Channel)
        composable("student_assignments") {
            StudentAssignmentDashboardNew(
                navController = navController,
                studentEmail = "student@example.com" // TODO: Get actual student email
            )
        }

        composable(
            "class_session/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.IntType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: 0
            ClassSessionScreen(navController = navController, classId = classId)
        }

        composable("view_all_classes") {
            ViewAllClassesScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }

        composable("student_settings") {
            StudentSettingsScreen(navController = navController)
        }

        // Teacher Assignment Dashboard
        composable(
            "teacher_assignments/{teacherEmail}/{classId}",
            arguments = listOf(
                navArgument("teacherEmail") { type = NavType.StringType },
                navArgument("classId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val teacherEmail = backStackEntry.arguments?.getString("teacherEmail") ?: ""
            val classId = backStackEntry.arguments?.getInt("classId") ?: 0
            TeacherAssignmentDashboardNew(
                navController = navController,
                teacherEmail = teacherEmail,
                classId = classId
            )
        }

        // Teacher Assignment Creation
        composable(
            "teacher_create_assignment/{teacherEmail}/{classId}",
            arguments = listOf(
                navArgument("teacherEmail") { type = NavType.StringType },
                navArgument("classId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val teacherEmail = backStackEntry.arguments?.getString("teacherEmail") ?: ""
            val classId = backStackEntry.arguments?.getInt("classId") ?: 0
            TeacherAssignmentCreationNew(
                navController = navController,
                teacherEmail = teacherEmail,
                classId = classId
            )
        }

        // Student Assignment View
        composable(
            "student_assignment_view/{assignmentId}",
            arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: ""
            StudentAssignmentViewNew(
                navController = navController,
                assignmentId = assignmentId,
                studentEmail = "student@example.com" // TODO: Get actual student email
            )
        }

        // Teacher Submission Review
        composable(
            "teacher_assignment_details/{assignmentId}/{teacherEmail}",
            arguments = listOf(
                navArgument("assignmentId") { type = NavType.StringType },
                navArgument("teacherEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: ""
            val teacherEmail = backStackEntry.arguments?.getString("teacherEmail") ?: ""
            TeacherSubmissionReviewNew(
                navController = navController,
                assignmentId = assignmentId,
                teacherEmail = teacherEmail
            )
        }
    }
}
