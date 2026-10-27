package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponIssue;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 管理优惠券发布、领取和使用校验，区分券定义与会员持有的券实例。 */
/**
 * 优惠券服务实现类
 * 实现优惠券的增删改查、审核、领取、核销等核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    /**
     * 优惠券数据访问层
     */
    private final CouponMapper couponMapper;
    /**
     * 优惠券发放记录数据访问层
     */
    private final CouponIssueMapper couponIssueMapper;

    @Override
    public IPage<Coupon> selectPage(Integer page, Integer size, Integer status, String keyword) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.hasText(keyword), Coupon::getCouponName, keyword)
                .orderByDesc(Coupon::getCreateTime);
        return couponMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID获取优惠券信息
     * @param id 优惠券ID
     * @return 返回对应的优惠券对象，如果不存在则返回null
     */
    @Override
    public Coupon getById(Long id) {
        // 调用couponMapper的selectById方法查询数据库
        return couponMapper.selectById(id);
    }

    /**
     * 保存优惠券信息
     * @param coupon 优惠券对象，包含优惠券的详细信息
     * 该方法使用事务注解，确保操作的原子性
     */
    @Override
    @Transactional  // 事务注解，保证数据库操作的原子性
    public void save(Coupon coupon) {
        // 规范化优惠券数据，确保数据格式正确
        normalizeCoupon(coupon);
        // 设置优惠券状态为0（通常表示未发布）
        coupon.setStatus(0);
        // 设置已发放数量为0
        coupon.setIssuedCount(0);
        // 将优惠券信息插入到数据库中
        couponMapper.insert(coupon);
    }

    /**
     * 更新优惠券信息
     * 该方法会重置部分字段，确保数据一致性
     * @param coupon 包含更新信息的优惠券对象
     */
    @Override
    @Transactional  // 使用事务注解，确保操作原子性
    public void update(Coupon coupon) {
        // 对优惠券数据进行规范化处理
        normalizeCoupon(coupon);


        // 重置不需要由外部更新的字段，这些字段应由系统内部控制
        coupon.setStatus(null);          // 状态不由外部更新
        coupon.setIssuedCount(null);     // 已发放数量不由外部更新
        coupon.setCreateTime(null);      // 创建时间不由外部更新
        coupon.setUpdateTime(null);      // 更新时间不由外部更新
        coupon.setDeleted(null);         // 删除标记不由外部更新
        // 调用Mapper执行数据库更新操作
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        couponMapper.deleteById(id);
    }

    /**
     * 提交优惠券审核
     * @param id 优惠券ID
     * @throws BusinessException 当优惠券不存在时抛出业务异常
     */
    @Override  // 标记重写父类方法
    @Transactional
    public void submitAudit(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        coupon.setStatus(1);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void audit(Long id, Integer status, String comment) {
        if (!Integer.valueOf(2).equals(status) && !Integer.valueOf(3).equals(status)) {
            throw new BusinessException("优惠券审核状态不合法");
        }
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        if (coupon.getStatus() != 1) throw new BusinessException("该优惠券不是待审核状态");
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    /**
     * 获取所有可用的优惠券列表
     * @return 返回可用优惠券的列表
     */
    @Override
    public List<Coupon> listAvailable() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 使用MyBatis-Plus的Lambda查询构造器查询符合条件的优惠券
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                // 筛选状态为2的优惠券（已发布）
                .eq(Coupon::getStatus, 2)
                .le(Coupon::getValidStart, now).ge(Coupon::getValidEnd, now)
                .and(wrapper -> wrapper.isNull(Coupon::getMaxIssue).or().le(Coupon::getMaxIssue, 0)
                        .or().ltSql(Coupon::getIssuedCount, "max_issue"))
                .orderByAsc(Coupon::getValidEnd));
    }

    /** 锁定券定义并条件递增发行数，避免并发领取突破总量；领取记录与计数在同一事务提交。 */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 声明式事务注解，表示该方法发生任何异常都会回滚事务
    public void claim(Long couponId, Long memberId) {
        // 使用selectForUpdate锁定优惠券记录，防止并发问题
        Coupon coupon = couponMapper.selectForUpdate(couponId);
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 检查优惠券是否可领取：优惠券存在、状态为可领取、在有效期内
        if (coupon == null || coupon.getStatus() != 2 || coupon.getValidStart().isAfter(now) || coupon.getValidEnd().isBefore(now)) {
            throw new BusinessException("优惠券不可领取");
        }
        // 查询该会员已领取的优惠券数量
        long claimed = couponIssueMapper.selectCount(Wrappers.<CouponIssue>lambdaQuery()
                .eq(CouponIssue::getCouponId, couponId).eq(CouponIssue::getMemberId, memberId));
        // 检查是否超过个人领取限制
        if (coupon.getPerLimit() != null && coupon.getPerLimit() > 0 && claimed >= coupon.getPerLimit()) {
            throw new BusinessException("已达到该优惠券领取上限");
        }
        // 增加优惠券已发放数量，并检查是否还有可用库存
        if (couponMapper.increaseIssuedCountIfAvailable(couponId) != 1) throw new BusinessException("优惠券已领完");
        // 创建优惠券发放记录
        CouponIssue issue = new CouponIssue();
        issue.setCouponId(couponId); issue.setMemberId(memberId); issue.setIssueTime(now); issue.setStatus(0);
        // 保存优惠券发放记录
        couponIssueMapper.insert(issue);
    }

    /**
     * 获取会员优惠券列表
     * @param memberId 会员ID
     * @return 会员优惠券列表
     */
    @Override
    public List<MemberCouponVO> listMemberCoupons(Long memberId) {
        // 根据会员ID查询优惠券发放记录，按发放时间降序排列
        List<CouponIssue> issues = couponIssueMapper.selectList(Wrappers.<CouponIssue>lambdaQuery()
                .eq(CouponIssue::getMemberId, memberId).orderByDesc(CouponIssue::getIssueTime));
        // 如果没有发放记录，返回空列表
        if (issues.isEmpty()) return List.of();
        // 批量查询优惠券信息，并转换为以优惠券ID为键的Map
        Map<Long, Coupon> coupons = couponMapper.selectBatchIds(issues.stream().map(CouponIssue::getCouponId).toList()).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        // 将优惠券发放记录转换为会员优惠券视图对象，并过滤掉空值
        return issues.stream().map(issue -> toMemberCoupon(issue, coupons.get(issue.getCouponId()))).filter(java.util.Objects::nonNull).toList();
    }

    private MemberCouponVO toMemberCoupon(CouponIssue issue, Coupon coupon) {
        if (coupon == null) return null;
        MemberCouponVO result = new MemberCouponVO();
        result.setIssueId(issue.getId()); result.setCouponId(coupon.getId()); result.setCouponName(coupon.getCouponName());
        result.setCouponType(coupon.getCouponType()); result.setThreshold(coupon.getThreshold()); result.setDiscount(coupon.getDiscount());
        result.setCurrency(coupon.getCurrency()); result.setValidStart(coupon.getValidStart()); result.setValidEnd(coupon.getValidEnd());
        LocalDateTime now = LocalDateTime.now();
        boolean active = Integer.valueOf(2).equals(coupon.getStatus())
                && coupon.getValidStart() != null && coupon.getValidEnd() != null
                && !coupon.getValidStart().isAfter(now) && !coupon.getValidEnd().isBefore(now);
        result.setStatus(Integer.valueOf(0).equals(issue.getStatus()) && !active ? 2 : issue.getStatus());
        return result;
    }

    /** 校验会员持有的有效券并计算折扣；这里只返回券实例 ID，实际核销由下单事务完成。 */
    @Override
    public DiscountResult validateAndCalculateDiscount(Long memberId, Long couponId, BigDecimal totalAmount) {
        if (couponId == null) return new DiscountResult(BigDecimal.ZERO.setScale(2), null);
        Coupon coupon = couponMapper.selectById(couponId);
        CouponIssue issue = couponIssueMapper.selectOne(Wrappers.<CouponIssue>lambdaQuery()
                .eq(CouponIssue::getCouponId, couponId)
                .eq(CouponIssue::getMemberId, memberId)
                .eq(CouponIssue::getStatus, 0)
                .orderByAsc(CouponIssue::getIssueTime)
                .orderByAsc(CouponIssue::getId)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || issue == null || !Integer.valueOf(2).equals(coupon.getStatus())
                || coupon.getValidStart() == null || coupon.getValidEnd() == null
                || coupon.getValidStart().isAfter(now) || coupon.getValidEnd().isBefore(now)) {
            throw new BusinessException("优惠券不可用");
        }
        BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
        if (totalAmount == null || totalAmount.compareTo(threshold) < 0) {
            throw new BusinessException("未满足优惠券使用门槛");
        }

        BigDecimal discount;
        if ("DISCOUNT".equals(coupon.getCouponType())) {
            if (coupon.getDiscount() == null || coupon.getDiscount().compareTo(BigDecimal.ZERO) < 0
                    || coupon.getDiscount().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessException("优惠券优惠规则无效");
            }
            discount = totalAmount.multiply(BigDecimal.TEN.subtract(coupon.getDiscount()))
                    .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
        } else if ("SHIPPING".equals(coupon.getCouponType())) {
            discount = BigDecimal.ZERO;
        } else {
            if (coupon.getDiscount() == null || coupon.getDiscount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("优惠券优惠规则无效");
            }
            discount = coupon.getDiscount().min(totalAmount);
        }
        return new DiscountResult(discount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP), issue.getId());
    }

    /**
     * 规范化优惠券信息
     * 对优惠券的各个属性进行标准化处理，确保数据的有效性和一致性
     * @param coupon 需要规范化的优惠券对象
     */
    private void normalizeCoupon(Coupon coupon) {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 规范化优惠券类型
        coupon.setCouponType(normalizeCouponType(coupon.getCouponType()));
        // 规范化优惠券使用范围
        coupon.setScope(normalizeScope(coupon.getScope()));
        // 设置默认货币类型为人民币
        if (coupon.getCurrency() == null) coupon.setCurrency("CNY");
        // 设置每人使用限制，如果未设置或小于1，则默认为1
        if (coupon.getPerLimit() == null || coupon.getPerLimit() < 1) coupon.setPerLimit(1);
        // 设置优惠券开始时间，如果未设置则为当前时间
        if (coupon.getValidStart() == null) coupon.setValidStart(now);
        // 设置优惠券结束时间，如果未设置则为开始时间后30天
        if (coupon.getValidEnd() == null) coupon.setValidEnd(coupon.getValidStart().plusDays(30));
        // 验证优惠券结束时间必须晚于开始时间，否则抛出业务异常
        if (!coupon.getValidEnd().isAfter(coupon.getValidStart())) throw new BusinessException("优惠券结束时间必须晚于开始时间");
    }

    private String normalizeCouponType(String value) {
        return switch (String.valueOf(value)) {
            case "1", "DISCOUNT" -> "DISCOUNT";
            case "2", "SHIPPING" -> "SHIPPING";
            default -> "FULL_REDUCTION";
        };
    }

    private String normalizeScope(String value) {
        return switch (String.valueOf(value)) {
            case "1", "CATEGORY" -> "CATEGORY";
            case "2", "SKU" -> "SKU";
            default -> "ALL";
        };
    }
}
