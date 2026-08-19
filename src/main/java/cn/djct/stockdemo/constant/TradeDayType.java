package cn.djct.stockdemo.constant;

/**
 * 交易日历日期类型。
 *
 */
public enum TradeDayType {

    /** 正常交易日。 */
    TRADING_DAY,

    /** 周末休市。 */
    WEEKEND,

    /** 法定节假日休市。 */
    NATIONAL_HOLIDAY,

    /** 交易所临时或特殊休市。 */
    EXCHANGE_CLOSED,


}
