package ditl.sim.pnt;

import ditl.Bus;
import ditl.sim.*;

public class PntRouterFactory implements RouterFactory {

	private final AdHocRadio adhoc_radio;
	private final PervasiveInfraRadio infra_radio;
	private final Bus<BufferEvent> _bus;
	private final InfraRouter infra_router;
	private final int buffer_size;
	
	public PntRouterFactory(InfraRouter infraRouter, PervasiveInfraRadio infraRadio, 
			AdHocRadio adhocRadio, int bufferSize, Bus<BufferEvent> bus){
		adhoc_radio = adhocRadio;
		infra_router = infraRouter;
		infra_radio = infraRadio;
		buffer_size = bufferSize;
		_bus = bus;
	}
	
	@Override
	public Router getNew(Integer id) {
		if ( id.equals(infra_router.id()) ){
			return infra_router;
		}
		return new PntRouter(adhoc_radio, infra_radio, id, buffer_size, _bus);
	}

}
