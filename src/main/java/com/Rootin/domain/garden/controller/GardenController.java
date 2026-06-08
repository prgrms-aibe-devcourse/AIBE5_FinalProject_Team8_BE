dpackage com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenLayoutUpdateRequest;
import com.Rootin.domain.garden.dto.GardenResponse;
import com.Rootin.domain.garden.dto.ThemeUpdateRequest;
import com.Rootin.domain.garden.service.GardenService;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/garden")
@RequiredArgsConstructor
public class GardenController {

    private final GardenService gardenService;

    /**
     * GET /api/v1/garden
     * 로그인한 사용자의 정원 테마 및 배치 정보(화분, 수확 식물)를 조회합니다.
     */
    @GetMapping
    public ResponseEntity<GardenResponse> getGarden(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        GardenResponse response = gardenService.getGarden(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/v1/garden/theme
     * 정원의 배경 테마를 변경합니다.
     */
    @PatchMapping("/theme")
    public ResponseEntity<Void> updateTheme(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody ThemeUpdateRequest request
    ) {
        gardenService.updateGardenTheme(userDetails.getUserId(), request.getTheme());
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/v1/garden/layout
     * 정원 내 화분과 식물의 배치 여부 및 좌표를 일괄 저장합니다.
     */
    @PutMapping("/layout")
    public ResponseEntity<Void> updateLayout(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody GardenLayoutUpdateRequest request
    ) {
        gardenService.updateGardenLayout(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
