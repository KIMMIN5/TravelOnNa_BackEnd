package com.travelonna.demo.domain.plan.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travelonna.demo.domain.plan.dto.PlaceResponseDto;
import com.travelonna.demo.domain.plan.dto.PlanDetailResponseDto;
import com.travelonna.demo.domain.plan.dto.PlanRequestDto.CreatePlanDto;
import com.travelonna.demo.domain.plan.dto.PlanRequestDto.UpdateLocationDto;
import com.travelonna.demo.domain.plan.dto.PlanRequestDto.UpdatePeriodDto;
import com.travelonna.demo.domain.plan.dto.PlanRequestDto.UpdatePlanDto;
import com.travelonna.demo.domain.plan.dto.PlanRequestDto.UpdateTransportDto;
import com.travelonna.demo.domain.plan.dto.PlanResponseDto;
import com.travelonna.demo.domain.plan.entity.Place;
import com.travelonna.demo.domain.plan.entity.Plan;
import com.travelonna.demo.domain.plan.repository.PlaceRepository;
import com.travelonna.demo.domain.plan.repository.PlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {
    
    private final PlanRepository planRepository;
    private final PlaceRepository placeRepository;
    
    /**
     * 개인 일정 생성
     */
    public PlanResponseDto createPlan(Integer userId, CreatePlanDto requestDto) {
        log.info("개인 일정 생성: 사용자 ID {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 null입니다.");
        }
        
        // 기간 유효성 검사
        validatePeriod(requestDto.getStartDate(), requestDto.getEndDate());
        
        Plan plan = new Plan();
        plan.setUserId(userId);
        plan.setTitle(requestDto.getTitle());
        plan.setStartDate(requestDto.getStartDate());
        plan.setEndDate(requestDto.getEndDate());
        plan.setLocation(requestDto.getLocation());
        plan.setTransportInfo(requestDto.getTransportInfo());
        plan.setIsPublic(requestDto.getIsPublic());
        plan.setMemo(requestDto.getMemo());
        plan.setTotalCost(0); // 초기 비용은 0으로 설정, 장소 추가 시 자동 계산됨
        
        // 그룹 ID가 있다면 설정
        if (requestDto.getGroupId() != null) {
            plan.setGroupId(requestDto.getGroupId());
            log.info("그룹 일정으로 생성: 그룹 ID {}", requestDto.getGroupId());
        }
        
        Plan savedPlan = planRepository.save(plan);
        log.info("일정이 생성되었습니다. ID: {}", savedPlan.getPlanId());
        
        return PlanResponseDto.fromEntity(savedPlan);
    }
    
    /**
     * 일정 여행지 업데이트
     */
    public PlanResponseDto updateLocation(Integer userId, Integer planId, UpdateLocationDto requestDto) {
        log.info("일정 여행지 업데이트: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        plan.updateLocation(requestDto.getLocation());
        
        Plan updatedPlan = planRepository.save(plan);
        log.info("일정 여행지가 업데이트되었습니다. ID: {}", updatedPlan.getPlanId());
        
        return PlanResponseDto.fromEntity(updatedPlan);
    }
    
    /**
     * 일정 이동수단 업데이트
     */
    public PlanResponseDto updateTransport(Integer userId, Integer planId, UpdateTransportDto requestDto) {
        log.info("일정 이동수단 업데이트: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        plan.updateTransport(requestDto.getTransportInfo());
        
        Plan updatedPlan = planRepository.save(plan);
        log.info("일정 이동수단이 업데이트되었습니다. ID: {}", updatedPlan.getPlanId());
        
        return PlanResponseDto.fromEntity(updatedPlan);
    }
    
    /**
     * 개인 일정 정보 업데이트
     */
    public PlanResponseDto updatePlan(Integer userId, Integer planId, UpdatePlanDto requestDto) {
        log.info("일정 정보 업데이트: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        
        // 새로운 기간이 유효한지 검사
        if (requestDto.getStartDate() != null && requestDto.getEndDate() != null) {
            validatePeriod(requestDto.getStartDate(), requestDto.getEndDate());
        }
        
        // 업데이트할 필드만 업데이트
        if (requestDto.getTitle() != null) plan.setTitle(requestDto.getTitle());
        if (requestDto.getStartDate() != null) plan.setStartDate(requestDto.getStartDate());
        if (requestDto.getEndDate() != null) plan.setEndDate(requestDto.getEndDate());
        if (requestDto.getLocation() != null) plan.setLocation(requestDto.getLocation());
        if (requestDto.getTransportInfo() != null) plan.setTransportInfo(requestDto.getTransportInfo());
        if (requestDto.getIsPublic() != null) plan.setIsPublic(requestDto.getIsPublic());
        if (requestDto.getMemo() != null) plan.setMemo(requestDto.getMemo());
        
        Plan updatedPlan = planRepository.save(plan);
        log.info("일정 정보가 업데이트되었습니다. ID: {}", updatedPlan.getPlanId());
        
        return PlanResponseDto.fromEntity(updatedPlan);
    }
    
    /**
     * 일정 기간 업데이트
     */
    public PlanResponseDto updatePeriod(Integer userId, Integer planId, UpdatePeriodDto requestDto) {
        log.info("일정 기간 업데이트: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        
        validatePeriod(requestDto.getStartDate(), requestDto.getEndDate());
        plan.updatePeriod(requestDto.getStartDate(), requestDto.getEndDate());
        
        Plan updatedPlan = planRepository.save(plan);
        log.info("일정 기간이 업데이트되었습니다. ID: {}", updatedPlan.getPlanId());
        
        return PlanResponseDto.fromEntity(updatedPlan);
    }
    
    /**
     * 일정 삭제
     */
    public void deletePlan(Integer userId, Integer planId) {
        log.info("일정 삭제: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        planRepository.delete(plan);
        
        log.info("일정이 삭제되었습니다. ID: {}", planId);
    }
    
    /**
     * 일정 비용 조회
     */
    @Transactional(readOnly = true)
    public Integer getPlanTotalCost(Integer userId, Integer planId) {
        log.info("일정 비용 조회: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        Integer totalCost = plan.getTotalCost();
        
        log.info("일정 총 비용: {}", totalCost);
        return totalCost != null ? totalCost : 0;
    }
    
    /**
     * 사용자의 개인 일정 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PlanResponseDto> getUserPlans(Integer userId) {
        log.info("사용자 일정 목록 조회: 사용자 ID {}", userId);
        
        List<Plan> plans = planRepository.findByUserId(userId);
        
        // 계획 엔티티를 DTO로 변환
        List<PlanResponseDto> result = plans.stream()
                .map(PlanResponseDto::fromEntity)
                .collect(Collectors.toList());
        
        log.info("사용자 일정 목록 조회 완료: 총 {}개의 일정", result.size());
        return result;
    }
    
    /**
     * 권한 검증이 포함된 일정 조회
     */
    private Plan getPlanWithPermissionCheck(Integer userId, Integer planId) {
        return planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없거나 권한이 없습니다: " + planId));
    }
    
    // 기간 유효성 검사 공통 메소드
    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 빨라야 합니다.");
        }
    }
    
    /**
     * 일정 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public PlanDetailResponseDto getPlanDetail(Integer userId, Integer planId) {
        log.info("일정 상세 정보 조회: 사용자 ID {}, 일정 ID {}", userId, planId);
        
        // 일정 존재 여부 확인 및 권한 체크
        Plan plan = getPlanWithPermissionCheck(userId, planId);
        
        // 해당 일정의 장소 목록 조회
        List<Place> places = placeRepository.findByPlanIdOrderByOrder(planId);
        
        // 장소 응답 DTO 생성 (visitDate로부터 여행 일차 계산)
        List<PlaceResponseDto> placeDtos = places.stream()
                .map(PlaceResponseDto::fromEntityWithDay)
                .collect(Collectors.toList());
        
        // 일정 상세 정보 응답 DTO 생성
        PlanDetailResponseDto responseDto = PlanDetailResponseDto.fromEntity(plan, placeDtos);
        
        log.info("일정 상세 정보 조회 완료: 일정 ID {}, 장소 수 {}", planId, placeDtos.size());
        
        return responseDto;
    }
} 