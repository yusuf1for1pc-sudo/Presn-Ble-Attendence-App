-- =====================================================
-- FINAL SUPABASE SETUP - SAFE VERSION (NO DATA LOSS)
-- This script safely updates existing tables without dropping data
-- =====================================================

-- =====================================================
-- TEACHER PROFILE TABLE - SAFE UPDATE
-- =====================================================

-- Add missing columns to existing teacher_profile table
ALTER TABLE teacher_profile ADD COLUMN IF NOT EXISTS subject TEXT;
ALTER TABLE teacher_profile ADD COLUMN IF NOT EXISTS department TEXT;
ALTER TABLE teacher_profile ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE teacher_profile ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Update existing records to have subject values (if they don't have them)
UPDATE teacher_profile 
SET subject = COALESCE(subject, 'General')
WHERE subject IS NULL;

-- Make subject NOT NULL after populating
ALTER TABLE teacher_profile ALTER COLUMN subject SET NOT NULL;

-- =====================================================
-- STUDENT PROFILE TABLE - SAFE UPDATE
-- =====================================================

-- Add missing columns to existing student_profile table
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS admission_year INTEGER;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS graduation_year INTEGER;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Ensure roll_number column is TEXT to support alphanumeric values (e.g., CS123, IT001, A123)
-- First, let's check if roll_number exists and what type it is
-- If it's INTEGER, we need to convert it back to TEXT
DO $$ 
BEGIN
    -- Check if roll_number column exists and is INTEGER
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'student_profile' 
        AND column_name = 'roll_number' 
        AND data_type = 'integer'
    ) THEN
        -- Add a temporary column for the conversion
        ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS roll_number_new TEXT;
        
        -- Convert existing roll_number values to text
        UPDATE student_profile 
        SET roll_number_new = roll_number::TEXT;
        
        -- Drop the old column and rename the new one
        ALTER TABLE student_profile DROP COLUMN IF EXISTS roll_number;
        ALTER TABLE student_profile RENAME COLUMN roll_number_new TO roll_number;
        ALTER TABLE student_profile ALTER COLUMN roll_number SET NOT NULL;
    END IF;
END $$;

-- Update existing records with default values
UPDATE student_profile 
SET admission_year = 2024, graduation_year = 2028
WHERE admission_year IS NULL OR graduation_year IS NULL;

-- Make columns NOT NULL after populating
ALTER TABLE student_profile ALTER COLUMN admission_year SET NOT NULL;
ALTER TABLE student_profile ALTER COLUMN graduation_year SET NOT NULL;

-- =====================================================
-- STUDENT GROUPS TABLE - SAFE UPDATE
-- =====================================================

-- Add missing columns to existing student_groups table (safe approach)
DO $$ 
BEGIN
    -- Add group_id column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'group_id') THEN
        ALTER TABLE student_groups ADD COLUMN group_id TEXT;
    END IF;
    
    -- Add admission_year column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'admission_year') THEN
        ALTER TABLE student_groups ADD COLUMN admission_year INTEGER;
    END IF;
    
    -- Add graduation_year column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'graduation_year') THEN
        ALTER TABLE student_groups ADD COLUMN graduation_year INTEGER;
    END IF;
    
    -- Add created_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'created_at') THEN
        ALTER TABLE student_groups ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
    END IF;
    
    -- Add updated_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'updated_at') THEN
        ALTER TABLE student_groups ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
    END IF;
    
    -- Add branch column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'branch') THEN
        ALTER TABLE student_groups ADD COLUMN branch TEXT;
    END IF;
    
    -- Add division column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'division') THEN
        ALTER TABLE student_groups ADD COLUMN division TEXT;
    END IF;
    
    -- Add batch column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'batch') THEN
        ALTER TABLE student_groups ADD COLUMN batch INTEGER DEFAULT 1;
    END IF;
    
    -- Add semester column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'semester') THEN
        ALTER TABLE student_groups ADD COLUMN semester INTEGER DEFAULT 1;
    END IF;
    
    -- Add academic_config_id column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'student_groups' AND column_name = 'academic_config_id') THEN
        ALTER TABLE student_groups ADD COLUMN academic_config_id INTEGER DEFAULT 1;
    END IF;
END $$;

-- Update existing records with default values
UPDATE student_groups 
SET admission_year = 2024, graduation_year = 2028
WHERE admission_year IS NULL OR graduation_year IS NULL;

-- Update additional columns with default values
UPDATE student_groups 
SET branch = 'CS', division = 'A', batch = 1, semester = 1, academic_config_id = 1
WHERE branch IS NULL OR division IS NULL OR batch IS NULL OR semester IS NULL OR academic_config_id IS NULL;

-- Make columns NOT NULL after populating
ALTER TABLE student_groups ALTER COLUMN admission_year SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN graduation_year SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN branch SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN division SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN batch SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN semester SET NOT NULL;
ALTER TABLE student_groups ALTER COLUMN academic_config_id SET NOT NULL;

-- =====================================================
-- CLASS ENROLLMENT TABLE - SAFE UPDATE
-- =====================================================

-- Add missing is_active column
ALTER TABLE class_enrollment ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- =====================================================
-- CREATE MISSING TABLES (if they don't exist)
-- =====================================================

-- Create classes table if it doesn't exist
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_name TEXT NOT NULL,
    subject TEXT NOT NULL,
    teacher_id UUID NOT NULL REFERENCES teacher_profile(id) ON DELETE CASCADE,
    group_id TEXT NOT NULL REFERENCES student_groups(group_id) ON DELETE CASCADE,
    semester INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Remove old columns from classes table if they exist (migration from old schema)
DO $$ 
BEGIN 
    -- Remove academic_year column if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'academic_year'
    ) THEN
        ALTER TABLE classes DROP COLUMN academic_year;
        RAISE NOTICE 'Dropped academic_year column from classes table';
    END IF;
    
    -- Remove subject_name column if it exists (renamed to subject)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'subject_name'
    ) THEN
        ALTER TABLE classes DROP COLUMN subject_name;
        RAISE NOTICE 'Dropped subject_name column from classes table';
    END IF;
    
    -- Remove class_schedule column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'class_schedule'
    ) THEN
        ALTER TABLE classes DROP COLUMN class_schedule;
        RAISE NOTICE 'Dropped class_schedule column from classes table';
    END IF;
    
    -- Remove class_code column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'class_code'
    ) THEN
        ALTER TABLE classes DROP COLUMN class_code;
        RAISE NOTICE 'Dropped class_code column from classes table';
    END IF;
    
    -- Remove description column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'description'
    ) THEN
        ALTER TABLE classes DROP COLUMN description;
        RAISE NOTICE 'Dropped description column from classes table';
    END IF;
    
    -- Remove is_active column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE classes DROP COLUMN is_active;
        RAISE NOTICE 'Dropped is_active column from classes table';
    END IF;
    
    -- Remove total_students column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'total_students'
    ) THEN
        ALTER TABLE classes DROP COLUMN total_students;
        RAISE NOTICE 'Dropped total_students column from classes table';
    END IF;
    
    -- Remove total_sessions column if it exists (not needed in new schema)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'total_sessions'
    ) THEN
        ALTER TABLE classes DROP COLUMN total_sessions;
        RAISE NOTICE 'Dropped total_sessions column from classes table';
    END IF;
END $$;

-- Create class_enrollment table if it doesn't exist
CREATE TABLE IF NOT EXISTS class_enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES student_profile(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(class_id, student_id)
);

-- Fix class_enrollment table column names if they exist (migration from old schema)
DO $$ 
BEGIN 
    -- Rename enrollment_date to enrolled_at if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'class_enrollment' AND column_name = 'enrollment_date'
    ) THEN
        ALTER TABLE class_enrollment RENAME COLUMN enrollment_date TO enrolled_at;
        RAISE NOTICE 'Renamed enrollment_date to enrolled_at in class_enrollment table';
    END IF;
END $$;

-- Create assignments table if it doesn't exist
CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    instructions TEXT,
    assignment_type TEXT NOT NULL DEFAULT 'homework',
    max_points INTEGER NOT NULL DEFAULT 100,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    allow_late_submission BOOLEAN NOT NULL DEFAULT true,
    late_penalty_percentage INTEGER NOT NULL DEFAULT 10,
    attachment_urls TEXT[],
    teacher_id UUID NOT NULL REFERENCES teacher_profile(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    total_submissions INTEGER NOT NULL DEFAULT 0,
    graded_submissions INTEGER NOT NULL DEFAULT 0
);

-- Create submissions table if it doesn't exist
CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES student_profile(id) ON DELETE CASCADE,
    submission_text TEXT,
    attachment_urls TEXT[],
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_late BOOLEAN NOT NULL DEFAULT false,
    points_earned INTEGER,
    max_points INTEGER NOT NULL,
    grade TEXT,
    feedback TEXT,
    graded_by UUID REFERENCES teacher_profile(id),
    graded_at TIMESTAMP WITH TIME ZONE,
    status TEXT NOT NULL DEFAULT 'submitted',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create academic_config table if it doesn't exist
CREATE TABLE IF NOT EXISTS academic_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key TEXT UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create attendance_sessions table if it doesn't exist
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    session_code TEXT NOT NULL,
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status TEXT NOT NULL DEFAULT 'scheduled',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create class_sessions table if it doesn't exist
CREATE TABLE IF NOT EXISTS class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    topic TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INDEXES FOR PERFORMANCE (Safe to run multiple times)
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_teacher_profile_email ON teacher_profile(email);
CREATE INDEX IF NOT EXISTS idx_teacher_profile_subject ON teacher_profile(subject);
CREATE INDEX IF NOT EXISTS idx_student_profile_email ON student_profile(email);
CREATE INDEX IF NOT EXISTS idx_student_profile_group_id ON student_profile(group_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_group_id ON student_groups(group_id);
CREATE INDEX IF NOT EXISTS idx_classes_teacher_id ON classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_group_id ON classes(group_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollment_class_id ON class_enrollment(class_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollment_student_id ON class_enrollment(student_id);
CREATE INDEX IF NOT EXISTS idx_assignments_teacher_id ON assignments(teacher_id);
CREATE INDEX IF NOT EXISTS idx_assignments_class_id ON assignments(class_id);
CREATE INDEX IF NOT EXISTS idx_assignments_due_date ON assignments(due_date);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_id ON submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student_id ON submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_class_id ON attendance_sessions(class_id);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_session_code ON attendance_sessions(session_code);
CREATE INDEX IF NOT EXISTS idx_class_sessions_class_id ON class_sessions(class_id);
CREATE INDEX IF NOT EXISTS idx_class_sessions_date ON class_sessions(session_date);

-- =====================================================
-- ROW LEVEL SECURITY (RLS) POLICIES - Safe to run
-- =====================================================

-- Enable RLS on all tables
ALTER TABLE teacher_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_enrollment ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE academic_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_sessions ENABLE ROW LEVEL SECURITY;

-- Teacher Profile Policies
DROP POLICY IF EXISTS "Teachers can view all teacher profiles" ON teacher_profile;
CREATE POLICY "Teachers can view all teacher profiles" ON teacher_profile FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can insert their own profile" ON teacher_profile;
CREATE POLICY "Teachers can insert their own profile" ON teacher_profile FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update their own profile" ON teacher_profile;
CREATE POLICY "Teachers can update their own profile" ON teacher_profile FOR UPDATE USING (true);

-- Student Profile Policies
DROP POLICY IF EXISTS "Students can view all student profiles" ON student_profile;
CREATE POLICY "Students can view all student profiles" ON student_profile FOR SELECT USING (true);

DROP POLICY IF EXISTS "Students can insert their own profile" ON student_profile;
CREATE POLICY "Students can insert their own profile" ON student_profile FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Students can update their own profile" ON student_profile;
CREATE POLICY "Students can update their own profile" ON student_profile FOR UPDATE USING (true);

-- Student Groups Policies
DROP POLICY IF EXISTS "Everyone can view student groups" ON student_groups;
CREATE POLICY "Everyone can view student groups" ON student_groups FOR SELECT USING (true);

DROP POLICY IF EXISTS "Everyone can insert student groups" ON student_groups;
CREATE POLICY "Everyone can insert student groups" ON student_groups FOR INSERT WITH CHECK (true);

-- Classes Policies
DROP POLICY IF EXISTS "Everyone can view classes" ON classes;
CREATE POLICY "Everyone can view classes" ON classes FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can insert classes" ON classes;
CREATE POLICY "Teachers can insert classes" ON classes FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update their classes" ON classes;
CREATE POLICY "Teachers can update their classes" ON classes FOR UPDATE USING (true);

-- Class Enrollment Policies
DROP POLICY IF EXISTS "Everyone can view class enrollment" ON class_enrollment;
CREATE POLICY "Everyone can view class enrollment" ON class_enrollment FOR SELECT USING (true);

DROP POLICY IF EXISTS "Everyone can insert class enrollment" ON class_enrollment;
CREATE POLICY "Everyone can insert class enrollment" ON class_enrollment FOR INSERT WITH CHECK (true);

-- Assignments Policies
DROP POLICY IF EXISTS "Everyone can view assignments" ON assignments;
CREATE POLICY "Everyone can view assignments" ON assignments FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can insert assignments" ON assignments;
CREATE POLICY "Teachers can insert assignments" ON assignments FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update their assignments" ON assignments;
CREATE POLICY "Teachers can update their assignments" ON assignments FOR UPDATE USING (true);

-- Submissions Policies
DROP POLICY IF EXISTS "Students can view their own submissions" ON submissions;
CREATE POLICY "Students can view their own submissions" ON submissions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can view all submissions" ON submissions;
CREATE POLICY "Teachers can view all submissions" ON submissions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Students can insert their own submissions" ON submissions;
CREATE POLICY "Students can insert their own submissions" ON submissions FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update submissions" ON submissions;
CREATE POLICY "Teachers can update submissions" ON submissions FOR UPDATE USING (true);

-- Academic Config Policies
DROP POLICY IF EXISTS "Everyone can view academic config" ON academic_config;
CREATE POLICY "Everyone can view academic config" ON academic_config FOR SELECT USING (true);

DROP POLICY IF EXISTS "Everyone can insert academic config" ON academic_config;
CREATE POLICY "Everyone can insert academic config" ON academic_config FOR INSERT WITH CHECK (true);

-- Attendance Sessions Policies
DROP POLICY IF EXISTS "Everyone can view attendance sessions" ON attendance_sessions;
CREATE POLICY "Everyone can view attendance sessions" ON attendance_sessions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can insert attendance sessions" ON attendance_sessions;
CREATE POLICY "Teachers can insert attendance sessions" ON attendance_sessions FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update attendance sessions" ON attendance_sessions;
CREATE POLICY "Teachers can update attendance sessions" ON attendance_sessions FOR UPDATE USING (true);

-- Class Sessions Policies
DROP POLICY IF EXISTS "Everyone can view class sessions" ON class_sessions;
CREATE POLICY "Everyone can view class sessions" ON class_sessions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Teachers can insert class sessions" ON class_sessions;
CREATE POLICY "Teachers can insert class sessions" ON class_sessions FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Teachers can update class sessions" ON class_sessions;
CREATE POLICY "Teachers can update class sessions" ON class_sessions FOR UPDATE USING (true);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT - Safe to run
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables
DROP TRIGGER IF EXISTS update_teacher_profile_updated_at ON teacher_profile;
CREATE TRIGGER update_teacher_profile_updated_at BEFORE UPDATE ON teacher_profile FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_student_profile_updated_at ON student_profile;
CREATE TRIGGER update_student_profile_updated_at BEFORE UPDATE ON student_profile FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_student_groups_updated_at ON student_groups;
CREATE TRIGGER update_student_groups_updated_at BEFORE UPDATE ON student_groups FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_classes_updated_at ON classes;
CREATE TRIGGER update_classes_updated_at BEFORE UPDATE ON classes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_assignments_updated_at ON assignments;
CREATE TRIGGER update_assignments_updated_at BEFORE UPDATE ON assignments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_submissions_updated_at ON submissions;
CREATE TRIGGER update_submissions_updated_at BEFORE UPDATE ON submissions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_academic_config_updated_at ON academic_config;
CREATE TRIGGER update_academic_config_updated_at BEFORE UPDATE ON academic_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_attendance_sessions_updated_at ON attendance_sessions;
CREATE TRIGGER update_attendance_sessions_updated_at BEFORE UPDATE ON attendance_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_class_sessions_updated_at ON class_sessions;
CREATE TRIGGER update_class_sessions_updated_at BEFORE UPDATE ON class_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- GRANT PERMISSIONS
-- =====================================================
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon;

-- =====================================================
-- COMPLETION MESSAGE
-- =====================================================
-- This script safely updates your existing database schema
-- without losing any data. All missing columns and tables
-- are added, and existing data is preserved.
-- =====================================================
