package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.security.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rentaltech.techrental.common.exception.TaskCreationException;
import com.rentaltech.techrental.common.exception.TaskNotFoundException;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.*;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.taskruleservice.TaskRuleService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCustomRepository taskCustomRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TaskDeliveryConfirmationRepository taskDeliveryConfirmationRepository;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskRuleService taskRuleService;

    @Override
    public Task createTask(TaskCreateRequestDto request, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        try {
            // Validate business rules
            validateTaskCreationRequest(request);
            TaskCategoryType category = request.getTaskCategory();

            // Tìm danh sách staff được assign (nếu có)
            Set<Staff> assignedStaff = resolveStaffMembers(request.getAssignedStaffIds());
            enforceDailyCapacity(assignedStaff, request.getPlannedStart(), null, category);

            Task task = Task.builder()
                    .taskCategory(category)
                    .orderId(request.getOrderId()) // Có thể null nếu là standalone task
                    .assignedStaff(assignedStaff)
                    .description(request.getDescription())
                    .plannedStart(request.getPlannedStart())
                    .plannedEnd(request.getPlannedEnd())
                    .build();

            Task saved = taskRepository.save(task);
            
            // Chỉ promote order status và notify customer nếu task gắn với order
            if (saved.getOrderId() != null) {
                promoteOrderStatusIfNeeded(saved);
                notifyCustomerOrderProcessing(saved);
            }
            
            notifyAssignedStaffChannels(saved, assignedStaff);

            return saved;

        } catch (IllegalArgumentException e) {
            throw new TaskCreationException("Không vượt qua kiểm tra hợp lệ: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TaskCreationException("Không thể tạo công việc: " + e.getMessage(), e);
        }
    }

    private void validateTaskCreationRequest(TaskCreateRequestDto request) {
        if (request.getTaskCategory() == null) {
            throw new IllegalArgumentException("Cần cung cấp TaskCategory");
        }
        // orderId là optional - có thể tạo task không gắn với order
        if (request.getPlannedStart() != null && request.getPlannedEnd() != null) {
            if (request.getPlannedStart().isAfter(request.getPlannedEnd())) {
                throw new IllegalArgumentException("Thời gian bắt đầu không được sau thời gian kết thúc dự kiến");
            }
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Không tìm thấy công việc với ID " + taskId));
    }

    @Override
    public Task getTaskById(Long taskId, String username) {
        Task task = getTaskById(taskId);
        AccessContext access = resolveAccessContext(username);
        enforceTaskVisibility(task, access);
        return task;
    }

    @Override
    public List<Task> getTasksByCategory(TaskCategoryType category) {
        if (category == null) {
            throw new IllegalArgumentException("Cần cung cấp category");
        }
        return taskRepository.findByTaskCategory(category);
    }

    @Override
    public List<Task> getTasksByOrder(Long orderId, String username) {
        if (orderId == null) {
            throw new IllegalArgumentException("Cần cung cấp orderId");
        }
        if (!rentalOrderRepository.existsById(orderId)) {
            throw new NoSuchElementException("Không tìm thấy đơn hàng");
        }
        AccessContext access = resolveAccessContext(username);
        List<Task> tasks = taskRepository.findByOrderId(orderId);
        return filterTasksForAccess(tasks, access);
    }

    @Override
    public List<Task> getTasks(TaskCategoryType category, Long orderId, Long assignedStaffId, String status, String username) {
        AccessContext access = resolveAccessContext(username);
        Long effectiveAssignedStaffId = resolveEffectiveAssignedStaff(assignedStaffId, access);

        if (orderId != null && !rentalOrderRepository.existsById(orderId)) {
            throw new NoSuchElementException("Không tìm thấy đơn hàng");
        }

        if (effectiveAssignedStaffId != null && !staffRepository.existsById(effectiveAssignedStaffId)) {
            throw new NoSuchElementException("Không tìm thấy nhân viên");
        }

        List<Task> tasks = taskRepository.findAll();
        if (category != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getTaskCategory() != null
                            && task.getTaskCategory() == category)
                    .collect(Collectors.toList());
        }
        if (orderId != null) {
            tasks = tasks.stream()
                    .filter(task -> orderId.equals(task.getOrderId()))
                    .collect(Collectors.toList());
        }
        if (effectiveAssignedStaffId != null) {
            tasks = tasks.stream()
                    .filter(task -> isTaskAssignedTo(task, effectiveAssignedStaffId))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            TaskStatus requestedStatus = parseTaskStatus(status);
            tasks = tasks.stream()
                    .filter(task -> task.getStatus() != null && task.getStatus() == requestedStatus)
                    .collect(Collectors.toList());
        }
        return filterTasksForAccess(tasks, access);
    }

    @Override
    public Page<Task> getTasksWithPagination(TaskCategoryType category, Long orderId, Long assignedStaffId, String status, String username, Pageable pageable) {
        try {
            AccessContext access = resolveAccessContext(username);
            Long effectiveAssignedStaffId = resolveEffectiveAssignedStaff(assignedStaffId, access);

        if (orderId != null && !rentalOrderRepository.existsById(orderId)) {
            throw new NoSuchElementException("Không tìm thấy đơn hàng");
        }

        if (effectiveAssignedStaffId != null && !staffRepository.existsById(effectiveAssignedStaffId)) {
            throw new NoSuchElementException("Không tìm thấy nhân viên");
        }

        TaskStatus taskStatus = null;
        if (status != null) {
            taskStatus = parseTaskStatus(status);
        }

        // Lấy tất cả tasks (không pagination) để sort theo khoảng cách tuyệt đối
        // Sau đó mới paginate
        List<Task> allTasks = taskCustomRepository.findTasksWithFilters(
                category,
                orderId,
                effectiveAssignedStaffId,
                taskStatus,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        // Filter by access (chỉ lấy tasks mà user có quyền xem)
        List<Task> filteredTasks = filterTasksForAccess(allTasks != null ? allTasks : new ArrayList<>(), access);
        
        // Null safety và tạo mutable list để có thể sort
        if (filteredTasks == null) {
            filteredTasks = new ArrayList<>();
        } else {
            // Tạo một ArrayList mới từ filteredTasks để có thể sort (tránh UnmodifiableList)
            filteredTasks = new ArrayList<>(filteredTasks);
        }
        
        // Sort theo khoảng cách tuyệt đối từ plannedEnd đến thời điểm hiện tại (gần nhất lên trước)
        // Ví dụ: Hôm nay 8/12 10h30, task có plannedEnd = 8/12 10h30 (0 phút) → lên đầu
        //        Task có plannedEnd = 8/12 11h00 (30 phút) → tiếp theo
        //        Task có plannedEnd = 8/12 09h00 (1h30) → xa hơn
        //        Task có plannedEnd = 7/12 10h30 (1 ngày) → xa nhất
        LocalDateTime now = LocalDateTime.now();
        filteredTasks.sort((t1, t2) -> {
            try {
                // Null safety cho task
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                
                LocalDateTime end1 = t1.getPlannedEnd();
                LocalDateTime end2 = t2.getPlannedEnd();
                
                // Nếu cả 2 đều null, sort theo createdAt DESC (null-safe)
                if (end1 == null && end2 == null) {
                    LocalDateTime created1 = t1.getCreatedAt();
                    LocalDateTime created2 = t2.getCreatedAt();
                    if (created1 == null && created2 == null) return 0;
                    if (created1 == null) return 1;
                    if (created2 == null) return -1;
                    return created2.compareTo(created1); // DESC
                }
                // Nếu t1 null, đưa xuống cuối
                if (end1 == null) return 1;
                // Nếu t2 null, đưa xuống cuối
                if (end2 == null) return -1;
                
                // Tính khoảng cách tuyệt đối (số giây)
                long distance1 = Math.abs(java.time.Duration.between(now, end1).getSeconds());
                long distance2 = Math.abs(java.time.Duration.between(now, end2).getSeconds());
                
                // Sort theo khoảng cách ASC (gần nhất lên trước)
                int compare = Long.compare(distance1, distance2);
                if (compare != 0) {
                    return compare;
                }
                // Nếu cùng khoảng cách, sort theo createdAt DESC (mới tạo lên trước)
                LocalDateTime created1 = t1.getCreatedAt();
                LocalDateTime created2 = t2.getCreatedAt();
                if (created1 == null && created2 == null) return 0;
                if (created1 == null) return 1;
                if (created2 == null) return -1;
                return created2.compareTo(created1); // DESC
            } catch (Exception e) {
                // Fallback: sort theo taskId nếu có lỗi
                Long id1 = t1.getTaskId();
                Long id2 = t2.getTaskId();
                if (id1 == null && id2 == null) return 0;
                if (id1 == null) return 1;
                if (id2 == null) return -1;
                return id1.compareTo(id2);
            }
        });
        
        // Apply pagination sau khi sort
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredTasks.size());
        List<Task> paginatedContent = start < filteredTasks.size() 
                ? filteredTasks.subList(start, end) 
                : new ArrayList<>();
        
        // Tạo Page mới với paginated content
        return new PageImpl<>(paginatedContent, pageable, filteredTasks.size());
        } catch (Exception e) {
            log.error("Error in getTasksWithPagination: username={}, category={}, orderId={}, assignedStaffId={}, status={}",
                    username, category, orderId, assignedStaffId, status, e);
            throw new RuntimeException("Lỗi khi lấy danh sách tác vụ: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    @Override
    public Task updateTask(Long taskId, TaskUpdateRequestDto request, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy công việc"));

        TaskCategoryType effectiveCategory = task.getTaskCategory();
        if (request.getTaskCategory() != null) {
            effectiveCategory = request.getTaskCategory();
            task.setTaskCategory(effectiveCategory);
        }

        Set<Staff> staffMembersForNotification = null;
        if (request.getAssignedStaffIds() != null) {
            Set<Staff> staffMembers = resolveStaffMembers(request.getAssignedStaffIds());
            LocalDateTime referenceDateTime = request.getPlannedStart() != null ? request.getPlannedStart() : task.getPlannedStart();
            enforceDailyCapacity(staffMembers, referenceDateTime, task.getTaskId(), effectiveCategory);
            task.setAssignedStaff(staffMembers);
            staffMembersForNotification = staffMembers;
        }
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPlannedStart() != null) task.setPlannedStart(request.getPlannedStart());
        if (request.getPlannedEnd() != null) task.setPlannedEnd(request.getPlannedEnd());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        Task saved = taskRepository.save(task);
        promoteOrderStatusIfNeeded(saved);
        notifyCustomerOrderProcessing(saved);
        notifyAssignedStaffChannels(saved, staffMembersForNotification);
        return saved;
    }

    @Override
    public void deleteTask(Long taskId, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        if (!taskRepository.existsById(taskId)) {
            throw new NoSuchElementException("Không tìm thấy công việc");
        }
        taskDeliveryConfirmationRepository.deleteByTask_TaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskCustomRepository.findOverdueTasks(LocalDateTime.now());
    }

    @Override
    public Task confirmDelivery(Long taskId, String username) {
        AccessContext access = resolveAccessContext(username);
        if (!access.isRestricted()) {
            throw new AccessDeniedException("Chỉ kỹ thuật viên hoặc nhân viên CSKH mới được xác nhận đi giao hàng");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy công việc"));
        enforceTaskVisibility(task, access);
        Staff confirmer = staffRepository.findById(access.staffId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên"));

        TaskDeliveryConfirmation confirmation = taskDeliveryConfirmationRepository
                .findByTask_TaskIdAndStaff_StaffId(taskId, confirmer.getStaffId())
                .orElseGet(() -> TaskDeliveryConfirmation.builder()
                        .task(task)
                        .staff(confirmer)
                        .build());
        confirmation.setConfirmedAt(LocalDateTime.now());
        taskDeliveryConfirmationRepository.save(confirmation);

        evaluateDeliveryConfirmation(task);
        return taskRepository.save(task);
    }

    @Override
    public Task confirmRetrieval(Long taskId, String username) {
        AccessContext access = resolveAccessContext(username);
        if (!access.isRestricted()) {
            throw new AccessDeniedException("Chỉ kỹ thuật viên hoặc nhân viên CSKH mới được xác nhận đi thu hồi");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy công việc"));
        enforceTaskVisibility(task, access);
        Staff confirmer = staffRepository.findById(access.staffId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên"));

        TaskDeliveryConfirmation confirmation = taskDeliveryConfirmationRepository
                .findByTask_TaskIdAndStaff_StaffId(taskId, confirmer.getStaffId())
                .orElseGet(() -> TaskDeliveryConfirmation.builder()
                        .task(task)
                        .staff(confirmer)
                        .build());
        confirmation.setConfirmedAt(LocalDateTime.now());
        taskDeliveryConfirmationRepository.save(confirmation);

        evaluateRetrievalConfirmation(task);
        return taskRepository.save(task);
    }

    @Override
    public List<com.rentaltech.techrental.staff.model.dto.StaffAssignmentDto> getStaffAssignmentsForDate(Long staffId,
                                                                                                         LocalDate targetDate,
                                                                                                         String username) {
        AccessContext access = resolveAccessContext(username);
        Long effectiveStaffId = resolveEffectiveAssignedStaff(staffId, access);
        if (effectiveStaffId == null) {
            throw new IllegalArgumentException("Cần cung cấp staffId");
        }
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        List<Task> tasks = taskRepository.findAssignmentsForStaffOnDate(effectiveStaffId, date);
        return tasks.stream()
                .map(com.rentaltech.techrental.staff.model.dto.StaffAssignmentDto::from)
                .toList();
    }

    @Override
    public List<com.rentaltech.techrental.staff.model.dto.TaskCompletionStatsDto> getMonthlyCompletionStats(int year,
                                                                                                            int month,
                                                                                                            TaskCategoryType category) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng không hợp lệ");
        }
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        List<Object[]> records = taskRepository.countCompletedTasksByCategory(start, end, category);
        return records.stream()
                .map(com.rentaltech.techrental.staff.model.dto.TaskCompletionStatsDto::fromRecord)
                .toList();
    }

    @Override
    public com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto getActiveTaskRule() {
        return taskRuleService.getActiveRule();
    }

    @Override
    public List<com.rentaltech.techrental.staff.model.dto.StaffTaskCountByCategoryDto> getStaffTaskCountByCategory(Long staffId, LocalDate targetDate, TaskCategoryType category, String username) {
        AccessContext access = resolveAccessContext(username);
        Long effectiveStaffId = resolveEffectiveAssignedStaff(staffId, access);
        if (effectiveStaffId == null) {
            throw new IllegalArgumentException("Không thể xác định staffId. Vui lòng đăng nhập hoặc cung cấp staffId");
        }
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        List<Object[]> records = taskCustomRepository.countActiveTasksByStaffCategoryAndDateGrouped(effectiveStaffId, date, category);
        return records.stream()
                .map(record -> {
                    TaskCategoryType cat = (TaskCategoryType) record[2];
                    com.rentaltech.techrental.staff.model.TaskRule rule = taskRuleService.getActiveRuleEntityByCategory(cat);
                    Integer maxTasksPerDay = (rule != null && rule.getMaxTasksPerDay() != null) ? rule.getMaxTasksPerDay() : null;
                    return com.rentaltech.techrental.staff.model.dto.StaffTaskCountByCategoryDto.from(record, maxTasksPerDay);
                })
                .toList();
    }

    private AccessContext resolveAccessContext(String username) {
        // Tối ưu: Lấy Account từ SecurityContext thay vì query lại database
        Account account = null;
        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                account = userDetails.getAccount();
            }
        } catch (Exception e) {
            // Fallback: query từ database nếu không lấy được từ SecurityContext
            log.debug("Could not get account from SecurityContext, querying database", e);
        }
        
        // Fallback: query từ database nếu không lấy được từ SecurityContext
        if (account == null) {
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username không được để trống");
            }
            account = accountService.getByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản với username: " + username));
        }
        
        if (account == null) {
            throw new NoSuchElementException("Không thể lấy thông tin tài khoản");
        }
        
        Role role = account.getRole();
        if (role == null) {
            throw new IllegalStateException("Tài khoản không có role");
        }
        
        if (role == Role.ADMIN || role == Role.OPERATOR) {
            return AccessContext.full(role);
        }
        if (role == Role.TECHNICIAN || role == Role.CUSTOMER_SUPPORT_STAFF) {
            if (account.getAccountId() == null) {
                throw new IllegalStateException("Tài khoản không có accountId");
            }
            Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());
            if (staff == null || staff.getStaffId() == null) {
                throw new NoSuchElementException("Không tìm thấy nhân viên cho accountId: " + account.getAccountId());
            }
            return AccessContext.restricted(role, staff.getStaffId());
        }
        throw new AccessDeniedException("Không có quyền truy cập với role: " + role);
    }

    private void ensureCanModify(AccessContext access) {
        if (!access.isFullAccess()) {
            throw new AccessDeniedException("Không có quyền thực hiện thao tác này");
        }
    }

    private void enforceTaskVisibility(Task task, AccessContext access) {
        if (!access.isRestricted()) {
            return;
        }
        if (!isTaskAssignedTo(task, access.staffId())) {
            throw new AccessDeniedException("Không có quyền truy cập tác vụ này");
        }
    }

    private void enforceDailyCapacity(Set<Staff> staffMembers, LocalDateTime plannedStart, Long excludeTaskId, TaskCategoryType taskCategory) {
        if (staffMembers == null || staffMembers.isEmpty()) {
            return;
        }
        if (taskCategory == null) {
            return;
        }
        
        TaskRule activeRule = taskRuleService.getActiveRuleEntityByCategory(taskCategory);
        if (activeRule == null || activeRule.getMaxTasksPerDay() == null || activeRule.getMaxTasksPerDay() <= 0) {
            return;
        }

        LocalDate targetDate = plannedStart != null ? plannedStart.toLocalDate() : LocalDate.now();
        int maxPerDay = activeRule.getMaxTasksPerDay();

        for (Staff staff : staffMembers) {
            if (staff == null || staff.getStaffId() == null) {
                continue;
            }

            long currentCount = taskCustomRepository.countActiveTasksByStaffCategoryAndDate(
                    staff.getStaffId(), taskCategory, targetDate, excludeTaskId);
            if (currentCount >= maxPerDay) {
                throw new IllegalStateException("Nhân viên " + staff.getStaffId()
                        + " (category=" + taskCategory + ") đã đạt giới hạn " + maxPerDay
                        + " công việc trong ngày " + targetDate
                        + " (hiện tại: " + currentCount + " công việc)");
            }
        }
    }

    private List<Task> filterTasksForAccess(List<Task> tasks, AccessContext access) {
        if (!access.isRestricted()) {
            return tasks;
        }
        Long staffId = access.staffId();
        return tasks.stream()
                .filter(task -> isTaskAssignedTo(task, staffId))
                .collect(Collectors.toList());
    }

    private Long resolveEffectiveAssignedStaff(Long requestedAssignedStaffId, AccessContext access) {
        if (!access.isRestricted()) {
            // ADMIN/OPERATOR: nếu không truyền staffId thì không thể xác định
            return requestedAssignedStaffId;
        }
        // TECHNICIAN/CUSTOMER_SUPPORT_STAFF: tự động lấy staffId của chính mình
        Long ownStaffId = access.staffId();
        if (requestedAssignedStaffId != null && !ownStaffId.equals(requestedAssignedStaffId)) {
            throw new AccessDeniedException("Không có quyền truy cập tác vụ của nhân viên khác");
        }
        // Nếu không truyền staffId, tự động dùng staffId của chính mình
        return ownStaffId;
    }

    private TaskStatus parseTaskStatus(String status) {
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Trạng thái tác vụ không hợp lệ: " + status, ex);
        }
    }

    private record AccessContext(Role role, Long staffId) {
        static AccessContext full(Role role) {
            return new AccessContext(role, null);
        }

        static AccessContext restricted(Role role, Long staffId) {
            return new AccessContext(role, staffId);
        }

        boolean isFullAccess() {
            return role == Role.ADMIN || role == Role.OPERATOR;
        }

        boolean isRestricted() {
            return staffId != null;
        }
    }

    private boolean isTaskAssignedTo(Task task, Long staffId) {
        if (task == null || staffId == null) {
            return false;
        }
        Set<Staff> assignees = task.getAssignedStaff();
        if (assignees == null || assignees.isEmpty()) {
            return false;
        }
        return assignees.stream().anyMatch(staff -> staff != null && staffId.equals(staff.getStaffId()));
    }

    private Set<Staff> resolveStaffMembers(List<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Staff> members = new LinkedHashSet<>();
        staffIds.stream()
                .filter(id -> id != null)
                .distinct()
                .forEach(id -> {
                    Staff staff = staffRepository.findById(id)
                            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên với id: " + id));
                    members.add(staff);
                });
        return members;
    }

    private void promoteOrderStatusIfNeeded(Task task) {
        if (task == null || task.getOrderId() == null) {
            return;
        }
        rentalOrderRepository.findById(task.getOrderId())
                .filter(order -> order.getOrderStatus() == OrderStatus.PENDING)
                .ifPresent(order -> {
                    order.setOrderStatus(OrderStatus.PROCESSING);
                    rentalOrderRepository.save(order);
                });
    }

    private void notifyCustomerOrderProcessing(Task task) {
        if (task.getOrderId() == null) {
            return; // Task không gắn với order, không cần notify customer
        }
        rentalOrderRepository.findById(task.getOrderId())
                .map(RentalOrder::getCustomer)
                .filter(customer -> customer != null && customer.getCustomerId() != null)
                .ifPresent(customer -> {
                    TaskCategoryType category = task.getTaskCategory();
                    NotificationType notificationType = NotificationType.ORDER_ACTIVE;
                    String body = "Đơn hàng của bạn đang được xử lý.";
                    if (category != null) {
                        notificationType = switch (category) {
                            case PRE_RENTAL_QC -> NotificationType.ORDER_PROCESSING;
                            case POST_RENTAL_QC -> NotificationType.ORDER_NEAR_DUE;
                            default -> NotificationType.ORDER_ACTIVE;
                        };
                        body = switch (category) {
                            case PRE_RENTAL_QC -> "Đơn hàng của bạn đang được phân công cho kỹ thuật viên chuẩn bị.";
                            case POST_RENTAL_QC -> "Đơn hàng của bạn đang được phân công cho kỹ thuật viên để thu hồi.";
                            default -> "Đơn hàng của bạn đang được xử lý.";
                        };
                    }
                    notificationService.notifyAccount(
                            customer.getAccount().getAccountId(),
                            notificationType,
                            "Đơn hàng đang được xử lý",
                            body
                    );
                });
    }

    private void notifyAssignedStaffChannels(Task task, Set<Staff> assignees) {
        if (task == null || assignees == null || assignees.isEmpty()) {
            return;
        }
        TaskAssignmentNotification payload = TaskAssignmentNotification.from(task);
        if (payload == null) {
            return;
        }
        assignees.stream()
                .filter(Objects::nonNull)
                .forEach(staff -> {
                    // Persist notification per staff account before sending
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.TASK_ASSIGNED,
                                "Tác vụ mới được gán",
                                "Bạn được gán tác vụ #" + payload.taskId() + " cho đơn #" + payload.orderId()
                        );
                    }
                    Long staffId = staff.getStaffId();
                    if (staffId == null) {
                        return;
                    }
                    resolveStaffChannels(staffId)
                            .forEach(destination -> sendToChannel(destination, payload, staffId));
                });
    }

    private List<String> resolveStaffChannels(Long staffId) {
        return List.of(String.format(STAFF_NOTIFICATION_TOPIC_TEMPLATE, staffId));
    }

    private void sendToChannel(String destination, TaskAssignmentNotification payload, Long staffId) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo gán tác vụ {} tới nhân viên {} qua {}: {}",
                    payload.taskId(), staffId, destination, ex.getMessage());
        }
    }

    private record TaskAssignmentNotification(Long taskId,
                                              Long orderId,
                                              TaskCategoryType taskCategory,
                                              String taskCategoryDisplayName,
                                              String description,
                                              TaskStatus status,
                                              LocalDateTime plannedStart,
                                              LocalDateTime plannedEnd,
                                              String message) {
        static TaskAssignmentNotification from(Task task) {
            if (task == null) {
                return null;
            }
            TaskCategoryType category = task.getTaskCategory();
            return new TaskAssignmentNotification(
                    task.getTaskId(),
                    task.getOrderId(),
                    category,
                    category != null ? category.getName() : null,
                    task.getDescription(),
                    task.getStatus(),
                    task.getPlannedStart(),
                    task.getPlannedEnd(),
                    buildMessage(task)
            );
        }

        private static String buildMessage(Task task) {
            String taskLabel = task.getTaskId() != null ? "#" + task.getTaskId() : "mới";
            String orderLabel = task.getOrderId() != null ? (" của đơn hàng #" + task.getOrderId()) : "";
            return "Bạn vừa được gán vào tác vụ " + taskLabel + orderLabel;
        }
    }

    private void evaluateDeliveryConfirmation(Task task) {
        if (task.getTaskId() == null) {
            return;
        }
        Set<Staff> assignedStaff = task.getAssignedStaff();
        if (assignedStaff == null || assignedStaff.isEmpty()) {
            return;
        }
        List<Long> assignedIds = assignedStaff.stream()
                .map(Staff::getStaffId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (assignedIds.isEmpty()) {
            return;
        }
        long confirmedCount = taskDeliveryConfirmationRepository
                .countByTask_TaskIdAndStaff_StaffIdIn(task.getTaskId(), assignedIds);
        if (confirmedCount < assignedIds.size()) {
            return;
        }
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            if (task.getPlannedStart() == null) {
                task.setPlannedStart(LocalDateTime.now());
            }
        }
        rentalOrderRepository.findById(task.getOrderId())
                .ifPresent(order -> {
                    if (order.getOrderStatus() != OrderStatus.DELIVERING) {
                        order.setOrderStatus(OrderStatus.DELIVERING);
                        rentalOrderRepository.save(order);
                    }
                });
    }

    private void evaluateRetrievalConfirmation(Task task) {
        if (task.getTaskId() == null) {
            return;
        }
        Set<Staff> assignedStaff = task.getAssignedStaff();
        if (assignedStaff == null || assignedStaff.isEmpty()) {
            return;
        }
        List<Long> assignedIds = assignedStaff.stream()
                .map(Staff::getStaffId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (assignedIds.isEmpty()) {
            return;
        }
        long confirmedCount = taskDeliveryConfirmationRepository
                .countByTask_TaskIdAndStaff_StaffIdIn(task.getTaskId(), assignedIds);
        if (confirmedCount < assignedIds.size()) {
            return;
        }
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            if (task.getPlannedStart() == null) {
                task.setPlannedStart(LocalDateTime.now());
            }
        }
    }
}
