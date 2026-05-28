package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WateringLogRepository wateringLogRepository;
    private final TilRepository tilRepository;
    private final TilTagRepository tilTagRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final LevelCalculator levelCalculator;

}
