package com.epam.esm.gifts.dao.impl;

import com.epam.esm.gifts.dao.GiftCertificateDao;
import com.epam.esm.gifts.dao.TagDao;
import com.epam.esm.gifts.dao.mapper.GiftCertificateExtractor;
import com.epam.esm.gifts.model.GiftCertificate;
import com.epam.esm.gifts.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static com.epam.esm.gifts.dao.constants.SqlQuery.*;

@Component
public class GiftCertificateDaoImpl implements GiftCertificateDao {

    private final JdbcTemplate jdbcTemplate;
    private final TagDao tagDao;
    private final GiftCertificateExtractor extractor;


    @Autowired
    public GiftCertificateDaoImpl(JdbcTemplate jdbcTemplate, TagDao tagDao, GiftCertificateExtractor extractor) {
        this.jdbcTemplate = jdbcTemplate;
        this.tagDao = tagDao;
        this.extractor = extractor;
    }

    @Override
    public List<GiftCertificate> show() {
        return jdbcTemplate.query(FIND_ALL_GIFT_CERTIFICATES,new BeanPropertyRowMapper<>(GiftCertificate.class));
    }

    @Override
    public GiftCertificate create(GiftCertificate certificate) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        certificate.setCreateDate(LocalDateTime.now());
        certificate.setLastUpdateDate(certificate.getCreateDate());

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(INSERT_GIFT_CERTIFICATE, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, certificate.getName());
            ps.setString(2, certificate.getDescription());
            ps.setBigDecimal(3, certificate.getPrice());
            ps.setInt(4, certificate.getDuration());
            ps.setTimestamp(5, Timestamp.valueOf(certificate.getCreateDate()));
            ps.setTimestamp(6, Timestamp.valueOf(certificate.getLastUpdateDate()));
            return ps;
        }, keyHolder);

        certificate.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return certificate;
    }

    @Override
    public Optional<GiftCertificate> findById(Long id) {
        List<GiftCertificate> giftCertificate = jdbcTemplate.query(FIND_GIFT_CERTIFICATE_BY_ID, extractor, id);
        return !giftCertificate.isEmpty() ? Optional.of(giftCertificate.get(0)) : Optional.empty();
    }
    /*@Override
    public Optional<GiftCertificate> findById(Long id) {
        Optional<GiftCertificate> optionalGiftCertificate = jdbcTemplate.query(FIND_GIFT_CERTIFICATE_BY_ID,new Object[]{id},
                        new BeanPropertyRowMapper<>(GiftCertificate.class))
                .stream().findAny();
        return !optionalGiftCertificate.isEmpty() ? optionalGiftCertificate : Optional.empty();
    }*/

    @Override
    public void update(long id,GiftCertificate updatedCertificate) {
        jdbcTemplate.update("UPDATE gift_certificate SET name=?,description=?,price=?,duration=?,create_date=?,last_update_date=? WHERE id=?",
                                   updatedCertificate.getName(),updatedCertificate.getDescription(),
                                   updatedCertificate.getPrice(), updatedCertificate.getDuration(),
                                   updatedCertificate.getCreateDate(),LocalDateTime.now(),
                                   id);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update(DELETE_GIFT_CERTIFICATE_BY_ID,id);
    }

    @Override
    public List<Tag> addTagsToCertificate(Long id, List<Tag> addedTagList) {
        addedTagList = addedTagList.stream().map(tagDao::findOrCreateTag).toList();
        for (Tag tag : addedTagList) {
            jdbcTemplate.update(con -> {
                PreparedStatement statement = con.prepareStatement(ADD_TAG_TO_GIFT_CERTIFICATE);
                statement.setLong(1, id);
                statement.setLong(2, tag.getId());
                return statement;
            });
        }
        return addedTagList;
    }

    @Override
    public boolean deleteAllTagsFromCertificate(Long id) {
        return jdbcTemplate.update(DELETE_ALL_TAGS_FROM_CERTIFICATE, id) > 0;
    }
}
