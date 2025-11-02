package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskCustomRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.common.exception.TaskNotFoundException;
import com.rentaltech.techrental.common.exception.TaskCreationException;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import com.rentaltech.techrental.webapi.customer.repository.RentalOrderRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    @Autowired
    private TaskCustomRepository taskCustomRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private StaffService staffService;

    @Override
    public Task createTask(TaskCreateRequestDto request, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        try {
            // Validate business rules
            validateTaskCreationRequest(request);
            
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory"));

            // Tìm staff được assign (nếu có)
            Staff assignedStaff = null;
            if (request.getAssignedStaffId() != null) {
                assignedStaff = staffRepository.findById(request.getAssignedStaffId())
                        .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên"));
            }

            Task task = Task.builder()
                    .taskCategory(category)
                    .orderId(request.getOrderId())
                    .assignedStaff(assignedStaff)
                    .type(request.getType())
                    .description(request.getDescription())
                    .plannedStart(request.getPlannedStart())
                    .plannedEnd(request.getPlannedEnd())
                    .build();

            Task saved = taskRepository.save(task);

            if (assignedStaff != null && request.getOrderId() != null) {
                rentalOrderRepository.findById(request.getOrderId())
                        .map(RentalOrder::getCustomer)
                        .filter(customer -> customer != null && customer.getCustomerId() != null)
                        .ifPresent(customer ->
                                notificationService.notifyCustomer(
                                        customer.getCustomerId(),
                                        NotificationType.ORDER_PROCESSING,
                                        "Đơn hàng đang được xử lý",
                                        "Đơn hàng của bạn đang được phân công cho kỹ thuật viên chuẩn bị.")
                        );
            }

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
                    .filter(task -> task.getAssignedStaff() != null
                            && effectiveAssignedStaffId.equals(task.getAssignedStaff().getStaffId()))
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

        if (request.getAssignedStaffId() != null) {
            Staff staff = staffRepository.findById(request.getAssignedStaffId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên"));
            task.setAssignedStaff(staff);
        }

        if (request.getTaskCategoryId() != null) {
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory"));
            task.setTaskCategory(category);
        }
        if (request.getType() != null) task.setType(request.getType());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPlannedStart() != null) task.setPlannedStart(request.getPlannedStart());
        if (request.getPlannedEnd() != null) task.setPlannedEnd(request.getPlannedEnd());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        return taskRepository.save(task);
    }

    @Override
    public void deleteTask(Long taskId, String username) {
        AccessContext access = resolveAccessContext(username);
        ensureCanModify(access);
        if (!taskRepository.existsById(taskId)) {
            throw new NoSuchElementException("Không tìm thấy công việc");
        }
        taskRepository.deleteById(taskId);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskCustomRepository.findOverdueTasks(LocalDateTime.now());
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
        if (task.getAssignedStaff() == null || !access.staffId().equals(task.getAssignedStaff().getStaffId())) {
            throw new AccessDeniedException("Không có quyền truy cập tác vụ này");
        }
    }

    private List<Task> filterTasksForAccess(List<Task> tasks, AccessContext access) {
        if (!access.isRestricted()) {
            return tasks;
        }
        Long staffId = access.staffId();
        return tasks.stream()
                .filter(task -> task.getAssignedStaff() != null
                        && staffId.equals(task.getAssignedStaff().getStaffId()))
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
}
