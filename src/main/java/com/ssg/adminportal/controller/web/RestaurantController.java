package com.ssg.adminportal.controller.web;

import com.ssg.adminportal.common.*;
import com.ssg.adminportal.domain.BusinessDay;
import com.ssg.adminportal.domain.Food;
import com.ssg.adminportal.domain.Restaurant;
import com.ssg.adminportal.dto.FileDTO;
import com.ssg.adminportal.dto.request.*;
import com.ssg.adminportal.service.FileService;
import com.ssg.adminportal.service.RestaurantService;
import com.ssg.adminportal.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/restaurants")
public class RestaurantController {

    @Value("${restaurant.image.dir}")
    private String restaurantDir;

    @Value("${food.image.dir}")
    private String foodDir;

    private final RestaurantService restaurantService;
    private final FileService fileService;
    private final ReviewService reviewService;

    @ModelAttribute("containFoodTypes")
    public ContainFoodType[] containFoodTypes() {
        return ContainFoodType.values();
    }

    @ModelAttribute("provideServiceTypes")
    public ProvideServiceType[] provideServiceTypes() {
        return ProvideServiceType.values();
    }

    @ModelAttribute("restaurantTypes")
    public RestaurantType[] restaurantTypes() {
        return RestaurantType.values();
    }

    @ModelAttribute("moodTypes")
    public MoodType[] moodTypes() {
        return MoodType.values();
    }


    @ModelAttribute("sortConditions")
    public Map<String, String> sortConditions() {
        Map<String, String> sortConditions = new HashMap<>();
        sortConditions.put("NEW", "최신 순");
        sortConditions.put("RATE", "평점 순");
        sortConditions.put("LIKE", "좋아요 순");
        sortConditions.put("REVIEW", "리뷰 순");
        return sortConditions;
    }


    @ModelAttribute("reservationTimeGaps")
    public ReservationTimeGap[] reservationGaps() {
        return ReservationTimeGap.values();
    }


    @ModelAttribute("dayOfWeeks")
    public List<String> dayOfWeeks() {
        List<String> dayOfWeeks = new ArrayList<>();
        dayOfWeeks.add("월요일");
        dayOfWeeks.add("화요일");
        dayOfWeeks.add("수요일");
        dayOfWeeks.add("목요일");
        dayOfWeeks.add("금요일");
        dayOfWeeks.add("토요일");
        dayOfWeeks.add("일요일");
        return dayOfWeeks;
    }

    @ModelAttribute("adminSortConditions")
    public Map<String , String> adminSortConditions(){
        Map<String, String> adminSortConditions = new HashMap<>();
        adminSortConditions.put("NEW", "최신 순");
        adminSortConditions.put("REGISTER", "등록 순");
        return adminSortConditions;
    }




    @GetMapping("/save")
    public String saveRestaurant(Model model) {
        model.addAttribute("restaurantSaveDto", new RestaurantSaveDTO());
        return "restaurant/saveForm";
    }

    @PostMapping("/save")
    @ResponseBody
    public String saveRestaurantPost(@ModelAttribute("restaurantSaveDto") RestaurantSaveDTO restaurantSaveDto, RedirectAttributes redirectAttributes) {
        log.info("restaurant = {}" , restaurantSaveDto.getRestaurantImages());
        log.info("food = {}", restaurantSaveDto.getFoodSaveDTOList());


        List<MultipartFile> restaurantImages = restaurantSaveDto.getRestaurantImages();
        List<MultipartFile> foodImages = new ArrayList<>();
        List<FoodSaveDTO> foodSaveDTOList = restaurantSaveDto.getFoodSaveDTOList();
        for (FoodSaveDTO foodSaveDto : foodSaveDTOList) {
            foodImages.add(foodSaveDto.getFoodImage());
        }

        /**
         * 식당 스토리지 저장 + DB에 스토리지 Url 저장
         */
        List<FileDTO> restaurantImagesFileDto = fileService.uploadFiles(restaurantImages, restaurantDir);
        List<String> restaurantImagesUrl = restaurantImagesFileDto.stream().map(FileDTO::getUploadFileUrl).toList();
        restaurantSaveDto.setRestaurantImagesUrl(restaurantImagesUrl);

        /**
         * 음식 스토리지 저장 + DB에 스토리지 Url 저장
         */
        List<FileDTO> foodImagesFileDto = fileService.uploadFiles(foodImages, foodDir);
        List<String> foodImagesUrl = foodImagesFileDto.stream().map(FileDTO::getUploadFileUrl).toList();
        for (String foodImageUrl : foodImagesUrl) {
            foodSaveDTOList.forEach(foodSaveDTO -> {
                foodSaveDTO.setFoodImageUrl(foodImageUrl);
            });
        };

        log.info("restaurantSaveDto = {}" , restaurantSaveDto);
        log.info("foodSaveDtoList = {}" , restaurantSaveDto.getFoodSaveDTOList());

        Long saveId = restaurantService.save(restaurantSaveDto);
        redirectAttributes.addAttribute("saveId", saveId);
        return "success";
    }


    @GetMapping("/{id}/update")
    public String updateRestaurant(@PathVariable("id") Long id ,  Model model) {
        Restaurant restaurant = restaurantService.findOne(id);
        String reservationTimeGap = convertReservationTimeGapToString(restaurant.getReservationTimeGap());


        RestaurantUpdateDTO restaurantUpdateDTO = new RestaurantUpdateDTO(restaurant.getName(), restaurant.getBusinessNum()
                , restaurant.getRestaurantTypes(), restaurant.getContainFoodTypes()
                , restaurant.getProvideServiceTypes(), restaurant.getMoodTypes(), restaurant.getAddress().getRoadAddress(),
                restaurant.getAddress().getLandLotAddress(), restaurant.getAddress().getZipCode(),
                restaurant.getAddress().getDetailAddress(), restaurant.getCanPark(),
                reservationTimeGap, restaurant.getIsPenalty());
        restaurantUpdateDTO.setRestaurantTypes(restaurant.getRestaurantTypes());
        restaurantUpdateDTO.setContainFoodTypes(restaurant.getContainFoodTypes());
        restaurantUpdateDTO.setMoodTypes(restaurant.getMoodTypes());
        restaurantUpdateDTO.setProvideServiceTypes(restaurant.getProvideServiceTypes());
        restaurantUpdateDTO.setRestaurantImagesUrl(Arrays.asList(restaurant.getRestaurantImages()));

        List<BusinessDay> businessDays = restaurant.getBusinessDays();
        List<String> dayOfWeeks = businessDays.stream().map(BusinessDay::getDayOfWeek).toList();
        List<LocalTime> openTimes = businessDays.stream().map(BusinessDay::getOpenTime).toList();
        List<LocalTime> closeTimes = businessDays.stream().map(BusinessDay::getCloseTime).toList();
        List<LocalTime> breakStartTimes = businessDays.stream().map(BusinessDay::getBreakStartTime).toList();
        List<LocalTime> breakEndTimes = businessDays.stream().map(BusinessDay::getBreakEndTime).toList();
        List<Boolean> isDayOffList = businessDays.stream().map(BusinessDay::getIsDayOff).toList(); //false , false , true , true , false , false

        restaurantUpdateDTO.setDayOfWeeks(dayOfWeeks);
        restaurantUpdateDTO.setOpenTimes(openTimes);
        restaurantUpdateDTO.setCloseTimes(closeTimes);
        restaurantUpdateDTO.setBreakStartTimes(breakStartTimes);
        restaurantUpdateDTO.setBreakEndTimes(breakEndTimes);
        restaurantUpdateDTO.setIsDayOffList(isDayOffList);

        List<Food> foods = restaurant.getFoods();
        List<FoodUpdateDTO> foodUpdateDTOList = new ArrayList<>();
        for (Food food : foods) {
            foodUpdateDTOList.add(new FoodUpdateDTO(food.getName() , food.getPrice() , food.getImage()));
        }
        restaurantUpdateDTO.setFoodSaveDtoList(foodUpdateDTOList);


        model.addAttribute("restaurantUpdateDTO", restaurantUpdateDTO);
        return "restaurant/updateForm";
    }


    private static String convertReservationTimeGapToString(Integer reservationTimeGap) {
        String convertReservationTimeGapToString = "";
        switch (reservationTimeGap) {
            case 30:
                convertReservationTimeGapToString =  "HALF";
                break;
            case 60:
                convertReservationTimeGapToString = "ONE";
                break;
            case 120:
                convertReservationTimeGapToString = "TWO";
                break;
        }

        return convertReservationTimeGapToString;
    }

    @GetMapping
    public String getAdminRestaurants(@ModelAttribute("restaurantAdminSearchCond") RestaurantAdminSearchCond restaurantAdminSearchCond , Model model) {
        model.addAttribute("restaurants", restaurantService.findRestaurantsAdmin(restaurantAdminSearchCond));
        return "restaurant/restaurants";
    }

    @GetMapping("/{id}")
    public String getAdminRestaurant(@PathVariable("id") Long id, Model model) {
        model.addAttribute("restaurant", restaurantService.findOne(id));
        model.addAttribute("sentiment", reviewService.getSentiment(id));
        return "restaurant/restaurant";
    }








}
