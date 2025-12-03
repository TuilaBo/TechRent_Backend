package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private TaskCategoryRepository taskCategoryRepository;

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
            
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory"));

            // Tìm danh sách staff được assign (nếu có)
            Set<Staff> assignedStaff = resolveStaffMembers(request.getAssignedStaffIds());
            enforceDailyCapacity(assignedStaff, request.getPlannedStart(), null);

            Task task = Task.builder()
                    .taskCategory(category)
                    .orderId(request.getOrderId())
                    .assignedStaff(assignedStaff)
                    .description(request.getDescription())
                    .plannedStart(request.getPlannedStart())
                    .plannedEnd(request.getPlannedEnd())
                    .build();

            Task saved = taskRepository.save(task);
            promoteOrderStatusIfNeeded(saved);
            notifyCustomerOrderProcessing(saved);
            notifyAssignedStaffChannels(saved, assignedStaff);

            return saved;
            
        } catch (IllegalArgumentException e) {
            throw new TaskCreationException("Không vượt qua kiểm tra hợp lệ: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TaskCreationException("Không thể tạo công việc: " + e.getMessage(), e);
        }
    }
    
    private void validateTaskCreationRequest(TaskCreateRequestDto request) {
        if (request.getTaskCategoryId() == null) {
            throw new IllegalArgumentException("Cần cung cấp TaskCategoryId");
        }
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Cần cung cấp OrderId");
        }
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
    public List<Task> getTasksByCategory(Long categoryId) {
        return taskRepository.findByTaskCategory_TaskCategoryId(categoryId);
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
    public List<Task> getTasks(Long categoryId, Long orderId, Long assignedStaffId, String status, String username) {
        AccessContext access = resolveAccessContext(username);
        Long effectiveAssignedStaffId = resolveEffectiveAssignedStaff(assignedStaffId, access);

        if (categoryId != null && !taskCategoryRepository.existsById(categoryId)) {
            throw new NoSuchElementException("Không tìm thấy TaskCategory");
        }

        if (orderId != null && !rentalOrderRepository.existsById(orderId)) {
            throw new NoSuchElementException("Không tìm thấy đơn hàng");
        }

        if (effectiveAssignedStaffId != null && !staffRepository.existsById(effectiveAssignedStaffId)) {
            throw new NoSuchElementException("Không tìm thấy nhân viên");
        }

        List<Task> tasks = taskRepository.findAll();
        if (categoryId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getTaskCategory() != null
                            && task.getTaskCategory().getTaskCategoryId().equals(categoryId))
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
    public Task updateTask(Long taskId, TaskUpdateRequestDto request, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy công việc"));

        Set<Staff> staffMembersForNotification = null;
        if (request.getAssignedStaffIds() != null) {
            Set<Staff> staffMembers = resolveStaffMembers(request.getAssignedStaffIds());
            LocalDateTime referenceDateTime = request.getPlannedStart() != null ? request.getPlannedStart() : task.getPlannedStart();
            enforceDailyCapacity(staffMembers, referenceDateTime, task.getTaskId());
            task.setAssignedStaff(staffMembers);
            staffMembersForNotification = staffMembers;
        }

        if (request.getTaskCategoryId() != null) {
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory"));
            task.setTaskCategory(category);
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
                                                                                                            Long categoryId) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng không hợp lệ");
        }
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        List<Object[]> records = taskRepository.countCompletedTasksByCategory(start, end, categoryId);
        return records.stream()
                .map(com.rentaltech.techrental.staff.model.dto.TaskCompletionStatsDto::fromRecord)
                .toList();
    }

    @Override
    public com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto getActiveTaskRule() {
        return taskRuleService.getActiveRule();
    }

    private AccessContext resolveAccessContext(String username) {
        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Role role = account.getRole();
        if (role == Role.ADMIN || role == Role.OPERATOR) {
            return AccessContext.full(role);
        }
        if (role == Role.TECHNICIAN || role == Role.CUSTOMER_SUPPORT_STAFF) {
            Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());
            return AccessContext.restricted(role, staff.getStaffId());
        }
        throw new AccessDeniedException("Không có quyền truy cập");
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

    private void enforceDailyCapacity(Set<Staff> staffMembers, LocalDateTime plannedStart, Long excludeTaskId) {
        if (staffMembers == null || staffMembers.isEmpty()) {
            return;
        }
        TaskRule activeRule = taskRuleService.getActiveRuleEntity();
        if (activeRule == null || activeRule.getMaxTasksPerDay() == null || activeRule.getMaxTasksPerDay() <= 0) {
            return;
        }
        LocalDate targetDate = plannedStart != null ? plannedStart.toLocalDate() : LocalDate.now();
        int maxPerDay = activeRule.getMaxTasksPerDay();
        for (Staff staff : staffMembers) {
            if (staff == null || staff.getStaffId() == null) {
                continue;
            }
            long currentCount = taskCustomRepository.countActiveTasksByStaffAndDate(staff.getStaffId(), targetDate, excludeTaskId);
            if (currentCount >= maxPerDay) {
                throw new IllegalStateException("Nhân viên " + staff.getStaffId()
                        + " đã đạt giới hạn " + maxPerDay + " công việc trong ngày " + targetDate
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
            return requestedAssignedStaffId;
        }
        Long ownStaffId = access.staffId();
        if (requestedAssignedStaffId != null && !ownStaffId.equals(requestedAssignedStaffId)) {
            throw new AccessDeniedException("Không có quyền truy cập tác vụ của nhân viên khác");
        }
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
        rentalOrderRepository.findById(task.getOrderId())
                .map(RentalOrder::getCustomer)
                .filter(customer -> customer != null && customer.getCustomerId() != null)
                .ifPresent(customer -> notificationService.notifyAccount(
                        customer.getAccount().getAccountId(),
                        switch (task.getTaskCategory().getName()) {
                            case "Pre rental QC" -> NotificationType.ORDER_PROCESSING;
                            case "Post rental QC" -> NotificationType.ORDER_NEAR_DUE;
                            default -> NotificationType.ORDER_ACTIVE;
                        },
                        "Đơn hàng đang được xử lý",
                        switch (task.getTaskCategory().getName()) {
                            case "Pre rental QC" -> "Đơn hàng của bạn đang được phân công cho kỹ thuật viên chuẩn bị.";
                            case "Post rental QC" -> "Đơn hàng của bạn đang được phân công cho kỹ thuật viên để thu hồi.";
                            default -> "Đơn hàng của bạn đang được xử lý.";
                        })
                );
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
                                              Long taskCategoryId,
                                              String taskCategoryName,
                                              String description,
                                              TaskStatus status,
                                              LocalDateTime plannedStart,
                                              LocalDateTime plannedEnd,
                                              String message) {
        static TaskAssignmentNotification from(Task task) {
            if (task == null) {
                return null;
            }
            TaskCategory category = task.getTaskCategory();
            return new TaskAssignmentNotification(
                    task.getTaskId(),
                    task.getOrderId(),
                    category != null ? category.getTaskCategoryId() : null,
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
