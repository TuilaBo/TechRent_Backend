package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.device.repository.*;
import com.rentaltech.techrental.device.service.AllocationSnapshotService;
import com.rentaltech.techrental.device.service.DeviceConditionService;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.devicereplacement.DeviceReplacementReportService;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportResponseDto;
import com.rentaltech.techrental.webapi.technician.repository.QCReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QCReportServiceImplTest {

    @Mock
    private QCReportRepository qcReportRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private AllocationConditionSnapshotRepository allocationConditionSnapshotRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private ConditionDefinitionRepository conditionDefinitionRepository;
    @Mock
    private AllocationSnapshotService allocationSnapshotService;
    @Mock
    private DeviceConditionService deviceConditionService;
    @Mock
    private AccountService accountService;
    @Mock
    private StaffService staffService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private BookingCalendarService bookingCalendarService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private DiscrepancyReportService discrepancyReportService;
    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;
    @Mock
    private CustomerComplaintRepository customerComplaintRepository;
    @Mock
    private DeviceReplacementReportService deviceReplacementReportService;

    private QCReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QCReportServiceImpl(
                qcReportRepository,
                taskRepository,
                deviceRepository,
                allocationConditionSnapshotRepository,
                orderDetailRepository,
                allocationRepository,
                conditionDefinitionRepository,
                allocationSnapshotService,
                deviceConditionService,
                accountService,
                staffService,
                notificationService,
                imageStorageService,
                bookingCalendarService,
                reservationService,
                rentalOrderRepository,
                messagingTemplate,
                discrepancyReportService,
                discrepancyReportRepository,
                customerComplaintRepository,
                deviceReplacementReportService
        );
    }

    @Test
    void getReportsByOrderReturnsEmptyWhenNoQcTasks() {
        Task nonQcTask = task(1L, "Delivery");
        when(taskRepository.findByOrderId(100L)).thenReturn(List.of(nonQcTask));

        List<QCReportResponseDto> result = service.getReportsByOrder(100L);

        assertThat(result).isEmpty();
        verify(qcReportRepository, never()).findByTask_TaskIdIn(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getReportsByOrderFetchesReportsForQcTasks() {
        Task preTask = task(10L, "Pre rental QC");
        Task postTask = task(11L, "POST RENTAL QC");
        when(taskRepository.findByOrderId(5L)).thenReturn(List.of(preTask, postTask));

        QCReport preReport = QCReport.builder()
                .qcReportId(null)
                .phase(QCPhase.PRE_RENTAL)
                .result(QCResult.READY_FOR_SHIPPING)
                .task(preTask)
                .build();
        QCReport postReport = QCReport.builder()
                .qcReportId(null)
                .phase(QCPhase.POST_RENTAL)
                .result(QCResult.READY_FOR_RE_STOCK)
                .task(postTask)
                .build();
        when(qcReportRepository.findByTask_TaskIdIn(List.of(10L, 11L)))
                .thenReturn(List.of(preReport, postReport));

        List<QCReportResponseDto> result = service.getReportsByOrder(5L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(QCReportResponseDto::getTaskId)
                .containsExactlyInAnyOrder(10L, 11L);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(qcReportRepository).findByTask_TaskIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L);
    }

    private Task task(Long id, String categoryName) {
        TaskCategory category = TaskCategory.builder()
                .taskCategoryId(id)
                .name(categoryName)
                .build();
        return Task.builder()
                .taskId(id)
                .taskCategory(category)
                .status(TaskStatus.PENDING)
                .build();
    }
}
