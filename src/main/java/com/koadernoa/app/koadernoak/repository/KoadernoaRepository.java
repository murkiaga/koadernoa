package com.koadernoa.app.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;

public interface KoadernoaRepository extends JpaRepository<Koadernoa, Long>{

	Optional<Koadernoa> findById(Long id);

	List<Koadernoa> findByIrakasleakContaining(Irakaslea irakaslea);
	
	//Tutore entitatearen arabera. Tutorearen taldeko koadernoak:
    List<Koadernoa> findByModuloa_Taldea_Tutorea(Irakaslea tutorea);
    //Tutorearen IDaren arabera
    List<Koadernoa> findByModuloa_Taldea_Tutorea_Id(Long tutoreId);
}
