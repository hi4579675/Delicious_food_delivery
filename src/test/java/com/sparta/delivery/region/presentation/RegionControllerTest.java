package com.sparta.delivery.region.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.region.application.RegionService;
import com.sparta.delivery.region.presentation.dto.RegionCreateRequest;
import com.sparta.delivery.region.presentation.dto.RegionResponse;
import com.sparta.delivery.region.presentation.dto.RegionUpdateRequest;
import com.sparta.delivery.user.application.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegionController.class)
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegionService regionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("지역 생성 API")
    class CreateRegionApiTest {

        @Test
        @DisplayName("MANAGER 권한이면 지역을 생성한다")
        void createRegion_success() throws Exception {
            // given
            RegionCreateRequest request = new RegionCreateRequest(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );
            RegionResponse response = new RegionResponse(
                    UUID.randomUUID(),
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionService.createRegion(any(RegionCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/regions")
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.regionCode").value("1100000000"))
                    .andExpect(jsonPath("$.data.regionName").value("서울특별시"));

            then(regionService).should().createRegion(any(RegionCreateRequest.class));
        }

        @Test
        @DisplayName("지역 코드 형식이 잘못되면 400을 반환한다")
        void createRegion_fail_whenRegionCodeInvalid() throws Exception {
            // given
            RegionCreateRequest request = new RegionCreateRequest(
                    "ABC",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            // when & then
            mockMvc.perform(post("/api/v1/regions")
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(regionService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("지역 조회 API")
    class GetRegionApiTest {

        @Test
        @DisplayName("지역 목록을 검색 파라미터로 조회한다")
        void searchRegions_success() throws Exception {
            // given
            RegionResponse response = new RegionResponse(
                    UUID.randomUUID(),
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionService.searchRegions("서울")).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/regions")
                            .param("keyword", "서울")
                            .with(authentication(managerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].regionCode").value("1100000000"))
                    .andExpect(jsonPath("$.data[0].regionName").value("서울특별시"));

            then(regionService).should().searchRegions("서울");
        }

        @Test
        @DisplayName("지역을 단건 조회한다")
        void getRegion_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();
            RegionResponse response = new RegionResponse(
                    regionId,
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionService.getRegion(regionId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/regions/{regionId}", regionId)
                            .with(authentication(managerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.regionId").value(regionId.toString()))
                    .andExpect(jsonPath("$.data.regionName").value("서울특별시"));

            then(regionService).should().getRegion(regionId);
        }

        @Test
        @DisplayName("최상위 지역 목록을 조회한다")
        void getRootRegions_success() throws Exception {
            // given
            RegionResponse response = new RegionResponse(
                    UUID.randomUUID(),
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionService.getRootRegions()).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/regions/root")
                            .with(authentication(managerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].regionCode").value("1100000000"))
                    .andExpect(jsonPath("$.data[0].parentId").doesNotExist());

            then(regionService).should().getRootRegions();
        }

        @Test
        @DisplayName("특정 지역의 하위 지역 목록을 조회한다")
        void getChildRegions_success() throws Exception {
            // given
            UUID parentId = UUID.randomUUID();
            RegionResponse response = new RegionResponse(
                    UUID.randomUUID(),
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            given(regionService.getChildRegions(parentId)).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/regions/{parentId}/children", parentId)
                            .with(authentication(managerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].regionCode").value("1111000000"))
                    .andExpect(jsonPath("$.data[0].parentId").value(parentId.toString()));

            then(regionService).should().getChildRegions(parentId);
        }
    }

    @Nested
    @DisplayName("지역 수정 API")
    class UpdateRegionApiTest {

        @Test
        @DisplayName("MANAGER 권한이면 지역을 수정한다")
        void updateRegion_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();
            RegionUpdateRequest request = new RegionUpdateRequest(
                    "서울특별시 수정",
                    null,
                    1,
                    true
            );
            RegionResponse response = new RegionResponse(
                    regionId,
                    "1100000000",
                    "서울특별시 수정",
                    null,
                    1,
                    true
            );

            given(regionService.updateRegion(eq(regionId), any(RegionUpdateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/regions/{regionId}", regionId)
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.regionName").value("서울특별시 수정"));

            then(regionService).should().updateRegion(eq(regionId), any(RegionUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("지역 삭제 API")
    class DeleteRegionApiTest {

        @Test
        @DisplayName("MASTER 권한이면 지역을 삭제한다")
        void deleteRegion_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/regions/{regionId}", regionId)
                            .with(csrf())
                            .with(authentication(masterAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(regionService).should().deleteRegion(regionId, 1L);
        }
    }

    private UsernamePasswordAuthenticationToken managerAuthentication() {
        return createAuthentication("MANAGER", "ROLE_MANAGER");
    }

    private UsernamePasswordAuthenticationToken masterAuthentication() {
        return createAuthentication("MASTER", "ROLE_MASTER");
    }

    private UsernamePasswordAuthenticationToken createAuthentication(String role, String authority) {
        TestUserPrincipal principal = new TestUserPrincipal(1L, role.toLowerCase(), role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    private record TestUserPrincipal(
            Long id,
            String username,
            String role
    ) implements UserPrincipal {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getRole() {
            return role;
        }
    }
}
