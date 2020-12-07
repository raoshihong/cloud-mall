List<AgentVo> agentVos;
        try {
            agentVos = ExcelUtils.getExcelData(file, AgentVo.templateName,AgentVo.class,AgentVo.template(),1001);
        } catch (BizBusinessException e){
            log.info("导入失败!{}",e);
            throw new BizBusinessException(e.getMessage());
        } catch (Exception e){
            log.info("导入失败! {}",e);
            throw new BizBusinessException("导入失败,请检查导入模板或数据是否正确");
        }
        
        


@Data
@ToString(callSuper = true)
public class AgentVo extends AgentBaseVo {
    private String agentName;
    private String achannelName;
    private String bchannelName;
    private String cchannelName;
    private String statusName;

    private static final LinkedHashMap<String, String> templateMap =
            new LinkedHashMap<String, String>();

    public static final String templateName = "客户经理导入模板";

    static{
        templateMap.put("achannelName","一级渠道");
        templateMap.put("bchannelName","二级渠道");
        templateMap.put("cchannelName","三级渠道");
        templateMap.put("name","客户经理姓名");
        templateMap.put("cnid","客户经理身份证号码");
        templateMap.put("mobile","客户经理手机号");
    }

    public static LinkedHashMap<String, String> template() {
        return templateMap;
    }

}


@Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        if(servletRequest.getMethod().equals(RequestMethod.OPTIONS.name())){
            return true;
        }else{
            return authenticatingFilterConfig.hasNoAuthentication(servletRequest);
        }
    }