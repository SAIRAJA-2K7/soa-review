package com.soareview.backend.config;

import com.soareview.backend.entity.*;
import com.soareview.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (userRepository.count() == 0) {
                UserEntity candidate = userRepository.save(UserEntity.builder()
                    .name("Alex Morgan")
                    .email("candidate@demo.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .verified(true)
                    .onboarded(true)
                    .avatar(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

                UserEntity recruiter = userRepository.save(UserEntity.builder()
                    .name("Sarah Chen")
                    .email("recruiter@demo.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ADMIN)
                    .verified(true)
                    .onboarded(true)
                    .avatar(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

                CompanyEntity company1 = companyRepository.save(CompanyEntity.builder()
                    .name("Nexus AI")
                    .logo("NA")
                    .industry("Artificial Intelligence")
                    .size("100-500")
                    .location("San Francisco, CA")
                    .build());

                CompanyEntity company2 = companyRepository.save(CompanyEntity.builder()
                    .name("Orbital Systems")
                    .logo("OS")
                    .industry("Aerospace & Defense")
                    .size("1000-5000")
                    .location("Seattle, WA")
                    .build());

                CompanyEntity company3 = companyRepository.save(CompanyEntity.builder()
                    .name("Meridian Health")
                    .logo("MH")
                    .industry("Healthcare Technology")
                    .size("200-500")
                    .location("Boston, MA")
                    .build());

                JobEntity productEngineer = jobRepository.save(JobEntity.builder()
                    .title("Senior Product Engineer")
                    .company(company1)
                    .location("San Francisco, CA")
                    .type(JobType.FULL_TIME)
                    .experience("5-8 years")
                    .salaryMin(160000)
                    .salaryMax(220000)
                    .postedAt(LocalDateTime.now().minusDays(12))
                    .description("Lead the architecture and development of our core AI infrastructure platform.")
                    .responsibilities(List.of("Design and implement scalable microservices", "Own the technical roadmap"))
                    .requirements(List.of("5+ years professional software engineering", "Strong communication skills"))
                    .skills(List.of("TypeScript", "React", "Node.js", "AWS", "System Design"))
                    .published(true)
                    .recruiter(recruiter)
                    .createdAt(LocalDateTime.now())
                    .build());

                JobEntity systemsArchitect = jobRepository.save(JobEntity.builder()
                    .title("Principal Systems Architect")
                    .company(company2)
                    .location("Seattle, WA")
                    .type(JobType.FULL_TIME)
                    .experience("10+ years")
                    .salaryMin(220000)
                    .salaryMax(300000)
                    .postedAt(LocalDateTime.now().minusDays(20))
                    .description("Architect the next generation of flight software systems for our orbital platforms.")
                    .responsibilities(List.of("Define architecture for flight software stack", "Lead safety and reliability initiatives"))
                    .requirements(List.of("10+ years aerospace software experience", "Expertise in real-time operating systems"))
                    .skills(List.of("C++", "RTOS", "Embedded Systems", "Flight Software", "Linux"))
                    .published(true)
                    .recruiter(recruiter)
                    .createdAt(LocalDateTime.now())
                    .build());

                JobEntity clinicalDataScientist = jobRepository.save(JobEntity.builder()
                    .title("Clinical Data Scientist")
                    .company(company3)
                    .location("Boston, MA")
                    .type(JobType.FULL_TIME)
                    .experience("3-5 years")
                    .salaryMin(110000)
                    .salaryMax(150000)
                    .postedAt(LocalDateTime.now().minusDays(8))
                    .description("Apply advanced data science techniques to analyze clinical trial data.")
                    .responsibilities(List.of("Analyze complex clinical datasets", "Build predictive models"))
                    .requirements(List.of("Masters or PhD in biostatistics", "Strong R and Python skills"))
                    .skills(List.of("Python", "R", "SQL", "Clinical Trials", "Biostatistics"))
                    .published(true)
                    .recruiter(recruiter)
                    .createdAt(LocalDateTime.now())
                    .build());

                applicationRepository.save(ApplicationEntity.builder()
                    .applicant(candidate)
                    .job(productEngineer)
                    .status(ApplicationStatus.INTERVIEW)
                    .appliedAt(LocalDateTime.now().minusDays(18))
                    .coverLetter("Excited to apply for the Senior Product Engineer role.")
                    .timeline(List.of(
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Applied")
                            .date(LocalDateTime.now().minusDays(18).toLocalDate().toString())
                            .completed(true)
                            .note("Application submitted successfully")
                            .build(),
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Shortlisted")
                            .date(LocalDateTime.now().minusDays(7).toLocalDate().toString())
                            .completed(true)
                            .note("Selected for technical interview")
                            .build(),
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Interview")
                            .date(LocalDateTime.now().minusDays(2).toLocalDate().toString())
                            .completed(true)
                            .note("Technical interview completed")
                            .build()))
                    .build());

                applicationRepository.save(ApplicationEntity.builder()
                    .applicant(candidate)
                    .job(systemsArchitect)
                    .status(ApplicationStatus.SHORTLISTED)
                    .appliedAt(LocalDateTime.now().minusDays(10))
                    .coverLetter("Interested in systems architecture roles.")
                    .timeline(List.of(
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Applied")
                            .date(LocalDateTime.now().minusDays(10).toLocalDate().toString())
                            .completed(true)
                            .note("Application submitted")
                            .build(),
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Resume Reviewed")
                            .date(LocalDateTime.now().minusDays(7).toLocalDate().toString())
                            .completed(true)
                            .note("Strong profile match")
                            .build()))
                    .build());

                applicationRepository.save(ApplicationEntity.builder()
                    .applicant(candidate)
                    .job(clinicalDataScientist)
                    .status(ApplicationStatus.UNDER_REVIEW)
                    .appliedAt(LocalDateTime.now().minusDays(5))
                    .coverLetter("Eager to contribute to clinical data science initiatives.")
                    .timeline(List.of(
                        ApplicationEntity.ApplicationTimelineItem.builder()
                            .stage("Applied")
                            .date(LocalDateTime.now().minusDays(5).toLocalDate().toString())
                            .completed(true)
                            .note("Application submitted")
                            .build()))
                    .build());
            }

            if (interviewRepository.count() == 0) {
                UserEntity candidate = userRepository.findByEmail("candidate@demo.com")
                    .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .name("Alex Morgan")
                        .email("candidate@demo.com")
                        .password(passwordEncoder.encode("password123"))
                        .role(Role.USER)
                        .verified(true)
                        .onboarded(true)
                        .avatar(null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

                JobEntity productEngineer = jobRepository.findAll().stream()
                    .filter(job -> "Senior Product Engineer".equals(job.getTitle()))
                    .findFirst()
                    .orElse(null);

                if (productEngineer == null) {
                    return;
                }

                interviewRepository.save(InterviewEntity.builder()
                    .candidate(candidate)
                    .job(productEngineer)
                    .scheduledAt(LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0))
                    .type("Technical")
                    .mode("Video Call")
                    .interviewer("Sarah Chen")
                    .status(com.soareview.backend.entity.InterviewStatus.SCHEDULED)
                    .build());
            }
        };
    }
}
